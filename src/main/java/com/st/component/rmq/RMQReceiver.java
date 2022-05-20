package com.st.component.rmq;
import com.rabbitmq.client.Channel;
import com.st.component.redis.RedisUtil;
import com.st.config.RabbitMQConfig;
import com.st.dao.OrderMapper;
import com.st.dao.ReceiptMapper;
import com.st.entity.po.Order;
import com.st.entity.to.Store;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Slf4j
public class RMQReceiver {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private RedisUtil redisUtil;


    @RabbitListener(queues = RabbitMQConfig.DB_ORDER_QUEUE)
    public void receiveOrder(Channel channel, Message message, Store store) throws IOException {
        Integer oid = orderMapper.insert(store.getOrder());
        store.getReceipt().setOid(oid);
        receiptMapper.insert(store.getReceipt());
    }

    @RabbitListener(queues = RabbitMQConfig.DEAD_ORDER_QUEUE)
    public void receiveExpired(Channel channel, Message message, Order order) throws IOException {
        redisUtil.incr("GC-"+order.getGid(),order.getNumber());
    }
}
