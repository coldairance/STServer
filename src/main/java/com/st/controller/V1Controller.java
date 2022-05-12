package com.st.controller;

import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.json.JSONUtil;
import com.st.dao.OrderMapper;
import com.st.dao.ReceiptMapper;
import com.st.entity.po.Good;
import com.st.entity.po.Order;
import com.st.entity.po.Receipt;
import com.st.component.rmq.RMQSender;
import com.st.component.redis.RedisUtil;
import com.st.result.Result;
import com.st.result.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;


@RestController
@RequestMapping("/v1")
public class V1Controller {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private RMQSender rmqSender;
    @Autowired
    private RedisUtil redisUtil;

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
    @PostMapping("/order")
    public Result addOrder(
            HttpServerRequest request,
            HttpServletResponse response,
            @RequestBody String data
    ){
        if(Math.random()>0.8) return new Result(ResultCode.SERVER_BUSY);
        Order order = JSONUtil.toBean(data, Order.class);
        order.setDiscount("1.0");

        // 购买不可重复校验
        Object o = redisUtil.get("GO-"+order.getUid()+"-"+order.getGid());
        if(o != null) return new Result(ResultCode.ORDER_REPETITION);

        // 查看库存
        Integer cnt = (Integer) redisUtil.get("GC-" + order.getGid());
        if(cnt<order.getNumber()) return new Result(ResultCode.GOOD_EMPTY);

        redisUtil.set("GO-"+order.getUid()+"-"+order.getGid(),order);
        // 订单失效时间，5s
        redisUtil.expire("GO-"+order.getUid()+"-"+order.getGid(),5);
        return new Result(ResultCode.SUCCESS);
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
    @PostMapping("/pay")
    public Result addReceiptMapper(
            HttpServerRequest request,
            HttpServletResponse response,
            @RequestBody String data
    ){
        Order order = JSONUtil.toBean(data, Order.class);

        // 订单失效校验
        order = (Order) redisUtil.get("GO-"+order.getUid()+"-"+order.getGid());
        if(order==null) return new Result(ResultCode.ORDER_EXPIRED);
        // 检查库存
        Integer cnt = (Integer) redisUtil.get("GC-" + order.getGid());
        if(cnt<order.getNumber()){
            return new Result(ResultCode.GOOD_EMPTY);
        }
        // 尝试获取商品
        synchronized (this){
            // 双重校验
            cnt = (Integer) redisUtil.get("GC-" + order.getGid());
            if(cnt<order.getNumber()){
                return new Result(ResultCode.GOOD_EMPTY);
            }
            redisUtil.decr("GC-" + order.getGid(),order.getNumber());
        }


        if(Math.random()>0.7){
            Integer discounts = (Integer) redisUtil.get("GD-" + order.getGid());
            if(discounts>0){
                synchronized (this){
                    discounts = (Integer) redisUtil.get("GD-" + order.getGid());
                    if(discounts>0){
                        order.setDiscount("0.85");
                        redisUtil.decr("GD-" + order.getGid(),1);
                    }
                }
            }
        }

        // 将订单置为秒杀时间段内存在（防范重复购买）
        redisUtil.set("GO-"+order.getUid()+"-"+order.getGid(),order);

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
