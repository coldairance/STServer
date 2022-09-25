package com.st.rabbitmq.component;
import com.rabbitmq.client.Channel;
import com.st.common.entity.po.Order;
import com.st.common.entity.to.Store;
import com.st.rabbitmq.config.RabbitMQConfig;
import com.st.service.mysql.MysqlService;
import com.st.service.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.sql.SQLException;


@Component
@Slf4j
public class RMQReceiver {

    @DubboReference
    private MysqlService mysqlService;


    @DubboReference
    private RedisService redisService;


    @RabbitListener(queues = RabbitMQConfig.DB_ORDER_QUEUE)
    public void receiveOrder(Channel channel, Message message, Store store) throws IOException {

        Order order = store.getOrder();
        try{
            mysqlService.store(store.getOrder(),store.getReceipt());
        }catch (RuntimeException e){
            // 补上库存
            redisService.increment("GC-"+order.getGid(),order.getNumber());
            // 删除订单
            redisService.del("PAY-" + order.getUid() + "-" + order.getGid());
        }
        // 查看是否缓存成功
        while(redisService.get("PAY-" + order.getUid() + "-" + order.getGid())==null){
            redisService.set("PAY-" + order.getUid() + "-" + order.getGid(),order);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DEAD_ORDER_QUEUE)
    public void receiveExpired(Channel channel, Message message, Order order) throws IOException {
        redisService.increment("GC-"+order.getGid(),order.getNumber());
    }
}
