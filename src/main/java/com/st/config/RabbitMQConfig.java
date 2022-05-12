package com.st.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    /**
     * Exchange
     */
    // 局部交换机
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("directExchange", true, false);
    }
    // 全局交换机
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("fanoutExchange",true,false);
    }
    // 死信交换机
    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange("deadExchange", true, false);
    }

    /**
     * Queue
     */
    // 存储数据库更新信息
    @Bean
    public Queue orderQueue(){
        return new Queue("db_order_queue",true);
    }

    /**
     * 死信队列
     */
    // 订单死信
    @Bean
    public Queue deadQueue(){
        return new Queue("dead_order_queue",true);
    }

    // 存储缓存过期订单信息
    @Bean
    public Queue expiredQueue(){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("x-message-ttl", 3000);//队列中所有消息n秒后过期
        map.put("x-dead-letter-exchange","deadExchange"); // 过期后去到死信交换机
        map.put("x-dead-letter-routing-key","dead_order"); // 死信队列路由
        return new Queue("expired_order_queue",true,false,false,map);
    }

    // 存储用户请求
    @Bean
    public Queue requestQueue(){
        return new Queue("request_queue",false);
    }
    // 存储全局消息
    @Bean
    public Queue allQueue(){
        return new Queue("all_queue", false);
    }

    /**
     * 绑定器
     */
    @Bean
    public Binding bindingOrder(){
        return BindingBuilder.bind(orderQueue()).to(directExchange()).with("db_order");
    }
    @Bean
    public Binding bindingDead(){
        return BindingBuilder.bind(deadQueue()).to(deadExchange()).with("dead_order");
    }
    @Bean
    public Binding bindingExpired(){
        return BindingBuilder.bind(expiredQueue()).to(directExchange()).with("expired_order");
    }
    @Bean
    public Binding bindingRequest(){
        return BindingBuilder.bind(requestQueue()).to(directExchange()).with("request");
    }
    @Bean
    public Binding bindingAll(){
        return BindingBuilder.bind(requestQueue()).to(fanoutExchange());
    }
}
