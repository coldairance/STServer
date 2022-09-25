package com.st.order.impl;
import cn.hutool.json.JSONUtil;
import com.st.common.component.SnowFlake;
import com.st.common.entity.po.Order;
import com.st.common.utils.Result;
import com.st.common.utils.ResultCode;
import com.st.order.config.Context;
import com.st.service.order.PlaceOrderService;
import com.st.service.rabbit.RabbitService;
import com.st.service.redis.RedisService;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@DubboService
public class PlaceOrderImpl implements PlaceOrderService {
    @DubboReference
    private RedisService redisService;
    @DubboReference
    private RabbitService rabbitService;
    @Autowired
    private Context context;
    @Autowired
    private SnowFlake snowFlake;
    @Value("${myself.expired}")
    private Integer expired;
    @Value("${myself.order-lock}")
    private String orderLock;

    @Override
    public Result order(String data) throws Exception {

        redisService.hashIncrement("order-instances",context.getDatacenterId()+":"+context.getMachineId(),1);

        Order order = JSONUtil.toBean(data, Order.class);
        order.setDiscount("1.0");

        // 购买不可重复校验
        Object o = redisService.get("GO-" + order.getUid() + "-" + order.getGid());
        if(o!=null) return new Result(ResultCode.ORDER_REPETITION);
        Object pay = redisService.get("PAY-" + order.getUid() + "-" + order.getGid());
        if(pay!=null) return new Result(ResultCode.PAY_REPETITION);
        order.setOid(snowFlake.nextId());
        // 预减操作（需要同步解决多卖问题）
        long id = Thread.currentThread().getId();
        if(redisService.lock(orderLock,id)){
            // 检查库存
            Integer cnt = (Integer) redisService.get("GC-" + order.getGid());
            if(cnt<order.getNumber()){
                return new Result(ResultCode.GOOD_EMPTY);
            }
            redisService.decrement("GC-" + order.getGid(),order.getNumber());
        }
        if(!redisService.unlock(orderLock,id)){
            return new Result(ResultCode.GOOD_EMPTY);
        }

        redisService.set("GO-"+order.getUid()+"-"+order.getGid(),order);
        redisService.expire("GO-"+order.getUid()+"-"+order.getGid(),expired);
        // 发送订单到过期队列
        rabbitService.sendExpiredOrder(order);
        return new Result(ResultCode.SUCCESS, order.getOid()+"");
    }
}

