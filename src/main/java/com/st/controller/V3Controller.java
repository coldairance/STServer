package com.st.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.json.JSONUtil;
import com.st.component.redis.RedisLimiter;
import com.st.component.rmq.RMQSender;
import com.st.component.socket.SocketSender;
import com.st.dao.OrderMapper;
import com.st.dao.ReceiptMapper;
import com.st.entity.po.Good;
import com.st.entity.po.Order;
import com.st.entity.po.Receipt;
import com.st.component.redis.RedisUtil;
import com.st.entity.to.Message;
import com.st.utils.Result;
import com.st.utils.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;


@RestController
@RequestMapping("/v3")
public class V3Controller {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private RedisLimiter redisLimiter;

    @Autowired
    private SocketSender socketSender;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RMQSender rmqSender;

    @Value("${myself.expired}")
    private Integer expired;

    @Value("${myself.limit.user.time}")
    private Integer limitTime;


    /**
     * data {
     *     uid,
     *     gid,
     *     number
     * }
     * @param request
     * @param response
     * @param data
     */
    @PostMapping("order")
    public Result addOrder(
            HttpServerRequest request,
            HttpServletResponse response,
            @RequestBody String data
    ){
        Order order = JSONUtil.toBean(data, Order.class);
        order.setDiscount("1.0");
        if(!redisLimiter.limit(""+order.getUid(),limitTime,1)){
            return new Result(ResultCode.SERVER_BUSY);
        }

        // 购买不可重复校验
        Object o = redisUtil.get("GO-" + order.getUid() + "-" + order.getGid());
        if(o!=null) return new Result(ResultCode.ORDER_REPETITION);
        Object pay = redisUtil.get("PAY-" + order.getUid() + "-" + order.getGid());
        if(pay!=null) return new Result(ResultCode.PAY_REPETITION);

        order.setUuid(IdUtil.simpleUUID());
        // 预减操作（需要同步解决多卖问题）
        synchronized (this){
            // 检查库存
            Integer cnt = (Integer) redisUtil.get("GC-" + order.getGid());
            if(cnt<order.getNumber()){
                return new Result(ResultCode.GOOD_EMPTY);
            }
            redisUtil.decr("GC-" + order.getGid(),order.getNumber());
        }

        redisUtil.set("GO-"+order.getUid()+"-"+order.getGid(),order);
        redisUtil.expire("GO-"+order.getUid()+"-"+order.getGid(),expired);
        // 发送订单到过期队列
        rmqSender.sendExpiredOrder(order);
        return new Result(ResultCode.SUCCESS, order.getUuid());
    }

    /**
     * data {
     *     uid,
     *     gid
     * }
     * @param request
     * @param response
     * @param data
     */
    @PostMapping("pay")
    public Result addReceiptMapper(
            HttpServerRequest request,
            HttpServletResponse response,
            @RequestBody String data
    ){
        Order order = JSONUtil.toBean(data, Order.class);
        // 订单失效校验
        Order cur = (Order) redisUtil.get("GO-" + order.getUid() + "-" + order.getGid());
        if(cur==null || !cur.getUuid().equals(order.getUuid()))
            return new Result(ResultCode.ORDER_EXPIRED);
        order = cur;

        synchronized (this){
            // 购买重复校验
            Object pay = redisUtil.get("PAY-" + order.getUid() + "-" + order.getGid());
            if(pay != null) return new Result(ResultCode.PAY_REPETITION);
            // 持久化订单
            redisUtil.set("PAY-" + order.getUid() + "-" + order.getGid(),order);
        }
        // 删减库存
        redisUtil.decr("GC-"+order.getGid(),order.getNumber());

        redisUtil.zIncrement("rank",order.getGid(),order.getNumber());
        // 发送排名消息
        socketSender.sendPublic(new Message(0,0,redisUtil.zRange("rank",0,-1)));
        // 发送抢购成功消息
        socketSender.sendPublic(new Message(order.getUid(),1,null));

        // 获取折扣
        if(Math.random()>0.7){
            Integer discounts = (Integer) redisUtil.get("GD-" + order.getGid());
            if(discounts>0){
                synchronized (this){
                    discounts = (Integer) redisUtil.get("GD-" + order.getGid());
                    if(discounts>0){
                        order.setDiscount("0.85");
                        redisUtil.decr("GD-" + order.getGid(),1);
                        // 发送获得优惠券成功消息
                        socketSender.sendPublic(new Message(order.getUid(),2,null));
                    }
                }
            }
        }

        // 插入收据，这里仅仅为了一致性判断
        Receipt receipt = new Receipt();
        receipt.setUid(order.getUid());
        Good good = (Good) redisUtil.get("GI-" + order.getGid());
        BigDecimal price = new BigDecimal(good.getPrice());
        BigDecimal number = new BigDecimal(order.getNumber());
        BigDecimal discount = new BigDecimal(order.getDiscount());
        receipt.setMoney(price.multiply(number).multiply(discount).toString());

        // 异步同步数据库
        rmqSender.sendDBOrder(order,receipt);
        return new Result(ResultCode.SUCCESS);
    }
}
