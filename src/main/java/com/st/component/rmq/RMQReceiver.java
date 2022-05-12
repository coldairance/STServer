package com.st.component.rmq;
import com.rabbitmq.client.Channel;
import com.st.component.redis.RedisUtil;
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

    @RabbitListener(queues = "db_order_queue")
    public void receiveOrder(Channel channel, Message message, Store store) throws IOException {
        Integer oid = orderMapper.insert(store.getOrder());
        store.getReceipt().setOid(oid);
        receiptMapper.insert(store.getReceipt());

        // 消息确认接收
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            if (message.getMessageProperties().getRedelivered()) {

                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {

                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // 返回队列
            }
        }
    }

    @RabbitListener(queues = "dead_order_queue")
    public void receiveExpired(Channel channel, Message message, Order order) throws IOException {

        String key = "GO-"+order.getUid()+"-"+order.getGid();
        synchronized (redisUtil){
            // // 检查是否已付款
            if(redisUtil.getExpire(key) != -1){
                // 增加库存
                redisUtil.incr("GC-"+order.getGid(),order.getNumber());
                System.out.println("增加："+order.getGid()+"-"+order.getNumber());
                // 删除
                redisUtil.del(key);
            }
        }

        // 消息确认接收
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            if (message.getMessageProperties().getRedelivered()) {

                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {

                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // 返回队列
            }
        }
    }
}
