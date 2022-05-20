package com.st.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit // 开启注解RabbitMQ
@Slf4j
public class RabbitMQConfig {
    @Autowired
    private ConnectionFactory connectionFactory;

    // 交换机
    public final static String DIRECT_EXCHANGE="directExchange";
    public final static String FANOUT_EXCHANGE="fanoutExchange";
    public final static String DEAD_EXCHANGE="deadExchange";

    // 队列
    public final static String DB_ORDER_QUEUE="db_order_queue";
    public final static String DEAD_ORDER_QUEUE="dead_order_queue";
    public final static String EXPIRED_ORDER_QUEUE="expired_order_queue";



    // 路由
    public final static String DB_ORDER_KEY="db_order";
    public final static String DEAD_ORDER_KEY="dead_order";
    public final static String EXPIRED_ORDER_KEY="expired_order";

    @Value("${myself.expired}")
    private Integer expired;


    /**
     * Exchange
     */
    // 局部交换机
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }
    // 全局交换机
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE,true,false);
    }
    // 死信交换机
    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    /**
     * Queue
     */
    // 存储数据库更新信息
    @Bean
    public Queue orderQueue(){
        return new Queue(DB_ORDER_QUEUE,true);
    }

    /**
     * 死信队列
     */
    // 订单死信
    @Bean
    public Queue deadQueue(){
        return new Queue(DEAD_ORDER_QUEUE,true);
    }

    // 存储缓存过期订单信息
    @Bean
    public Queue expiredQueue(){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("x-message-ttl", expired*1000);//队列中所有消息n秒后过期
        map.put("x-dead-letter-exchange","deadExchange"); // 过期后去到死信交换机
        map.put("x-dead-letter-routing-key","dead_order"); // 死信队列路由
        return new Queue(EXPIRED_ORDER_QUEUE,true,false,false,map);
    }

    /**
     * 绑定器
     */
    @Bean
    public Binding bindingOrder(){
        return BindingBuilder.bind(orderQueue()).to(directExchange()).with(DB_ORDER_KEY);
    }
    @Bean
    public Binding bindingDead(){
        return BindingBuilder.bind(deadQueue()).to(deadExchange()).with(DEAD_ORDER_KEY);
    }

    @Bean
    public Binding bindingExpired(){
        return BindingBuilder.bind(expiredQueue()).to(directExchange()).with(EXPIRED_ORDER_KEY);
    }



    /**
     * 消息发送前置处理
     * @return
     */
    @Bean
    public RabbitTemplate createRabbitTemplate(){

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 消息未能投递到 broker 中
        rabbitTemplate.setReturnsCallback(returnedMessage -> log.info("returnedMessage ===> replyCode={} ,replyText={} ,exchange={} ,routingKey={}", returnedMessage.getReplyCode(), returnedMessage.getReplyText(), returnedMessage.getExchange(), returnedMessage.getRoutingKey()));

        // 消息成功投递到 broker 中
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {

            // 打印异常信息
            if (!ack) {
                log.error("消息发送异常!correlationData={}, cause={}",correlationData.getId(),cause);
            }
        });

        return rabbitTemplate;
    }
}
