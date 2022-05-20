package com.st.component.rmq;

import com.st.entity.po.Order;
import com.st.entity.po.Receipt;
import com.st.entity.to.Store;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
public class RMQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送需要数据库存储的数据
     * @param order
     * @param receipt
     */
    public void sendDBOrder(Order order, Receipt receipt){
        rabbitTemplate.convertAndSend("directExchange","db_order",new Store(order,receipt),message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                new CorrelationData(UUID.randomUUID().toString())
                );
    }

    /**
     * 发送设置了过期时间的订单
     * @param order
     */
    public void sendExpiredOrder(Order order){


        rabbitTemplate.convertAndSend("directExchange","expired_order",order,message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                new CorrelationData(UUID.randomUUID().toString())
        );
    }
}
