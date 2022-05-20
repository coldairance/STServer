package com.st.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker //开启 WebSocket 代理
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 暴露节点
     * @param registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*")
                .withSockJS();
        WebSocketMessageBrokerConfigurer.super.registerStompEndpoints(registry);
    }

    /**
     * 使用 RabbitMQ 作为消息代理
     * @param registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // "STOMP broker relay"处理所有消息将消息发送到外部的消息代理
        registry.enableStompBrokerRelay("/exchange","/topic","/queue","/amq/queue")
                //.setVirtualHost("JCChost") //对应自己rabbitmq里的虚拟host
                .setRelayHost("127.0.0.1")
                .setClientLogin("guest")
                .setClientPasscode("guest")
                .setSystemLogin("guest")
                .setSystemPasscode("guest")
                .setSystemHeartbeatSendInterval(1000)
                .setSystemHeartbeatReceiveInterval(1000);

        WebSocketMessageBrokerConfigurer.super.configureMessageBroker(registry);
    }

    /**
     * 自定义拦截器
     * @param registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        WebSocketMessageBrokerConfigurer.super.configureClientInboundChannel(registration);
    }
}
