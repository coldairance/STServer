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

    @Autowired
    private ConfirmCallbackService confirmCallbackService;

    @Autowired
    private ReturnCallbackService returnCallbackService;

    /**
     * 发送需要数据库存储的数据
     * @param order
     * @param receipt
     */
    public void sendDBOrder(Order order, Receipt receipt){


        /**
         * 消费者确认收到消息后，手动ack回执回调处理
         */
        rabbitTemplate.setConfirmCallback(confirmCallbackService);

        /**
         * 消息投递到队列失败回调处理
         */
        rabbitTemplate.setReturnsCallback(returnCallbackService);

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


        /**
         * 消费者确认收到消息后，手动ack回执回调处理
         */
        rabbitTemplate.setConfirmCallback(confirmCallbackService);

        /**
         * 消息投递到队列失败回调处理
         */
        rabbitTemplate.setReturnsCallback(returnCallbackService);

        rabbitTemplate.convertAndSend("directExchange","expired_order",order,message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                new CorrelationData(UUID.randomUUID().toString())
        );
    }

}
