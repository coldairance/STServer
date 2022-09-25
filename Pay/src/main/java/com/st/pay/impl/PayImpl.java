package com.st.pay.impl;

import cn.hutool.json.JSONUtil;
import com.st.common.component.SnowFlake;
import com.st.common.entity.po.Good;
import com.st.common.entity.po.Order;
import com.st.common.entity.po.Receipt;
import com.st.common.entity.to.Message;
import com.st.common.utils.Result;
import com.st.common.utils.ResultCode;
import com.st.pay.config.Context;
import com.st.service.mysql.MysqlService;
import com.st.service.pay.PayOrderService;
import com.st.service.rabbit.RabbitService;
import com.st.service.redis.RedisService;
import com.st.service.socket.SocketService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;

@DubboService
public class PayImpl implements PayOrderService {
    @DubboReference
    private RedisService redisService;

    @DubboReference
    private RabbitService rabbitService;

    @DubboReference
    private SocketService socketService;

    @Autowired
    private Context context;
    @Autowired
    private SnowFlake snowFlake;

    @Value("${myself.repertory-lock}")
    private String repertoryLock;

    @Value("${myself.discount-lock}")
    private String discountLock;


    @Override
    public Result pay(String data) {

        redisService.hashIncrement("pay-instances",context.getDatacenterId()+":"+context.getMachineId(),1);

        Order order = JSONUtil.toBean(data, Order.class);
        // 订单失效校验
        Order cur = (Order) redisService.get("GO-" + order.getUid() + "-" + order.getGid());
        if(cur==null || !cur.getOid().equals(order.getOid()))
            return new Result(ResultCode.ORDER_EXPIRED);
        order = cur;

        long id = Thread.currentThread().getId();
        if(redisService.lockWithDog(repertoryLock,id)){
            // 购买重复校验
            Object pay = redisService.get("PAY-" + order.getUid() + "-" + order.getGid());
            if(pay != null) return new Result(ResultCode.PAY_REPETITION);
            // 持久化订单
            redisService.set("PAY-" + order.getUid() + "-" + order.getGid(),order);
        }
        redisService.unlock(repertoryLock,id);
        // 删减库存
        redisService.decrement("GC-"+order.getGid(),order.getNumber());

        redisService.zsetIncrement("rank",order.getGid(),order.getNumber());
        // 发送排名消息
        socketService.sendPublic(new Message(0,0,redisService.zsetRange("rank",0,-1)));
        // 发送抢购成功消息
        socketService.sendPublic(new Message(order.getUid(),1,null));

        // 获取折扣
        if(Math.random()>0.7){
            Integer discounts = (Integer) redisService.get("GD-" + order.getGid());
            if(discounts>0){
                if(redisService.lockWithDog(discountLock,id)){
                    discounts = (Integer) redisService.get("GD-" + order.getGid());
                    if(discounts>0){
                        order.setDiscount("0.85");
                        redisService.decrement("GD-" + order.getGid(),1);
                        // 发送获得优惠券成功消息
                        socketService.sendPublic(new Message(order.getUid(),2,null));
                    }
                }
                redisService.unlock(discountLock,id);
            }
        }

        // 插入收据，这里仅仅为了一致性判断
        Receipt receipt = new Receipt();
        receipt.setRid(snowFlake.nextId());
        receipt.setUid(order.getUid());
        receipt.setOid(order.getOid());
        Good good = (Good) redisService.get("GI-" + order.getGid());
        BigDecimal price = new BigDecimal(good.getPrice());
        BigDecimal number = new BigDecimal(order.getNumber());
        BigDecimal discount = new BigDecimal(order.getDiscount());
        receipt.setMoney(price.multiply(number).multiply(discount).toString());

        // 异步同步数据库
        rabbitService.sendDBOrder(order,receipt);
        return new Result(ResultCode.SUCCESS);
    }
}
