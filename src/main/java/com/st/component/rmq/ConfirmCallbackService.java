package com.st.component.rmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息只要被 rabbitmq broker 接收到就会触发 confirmCallback 回调 。
 */
@Slf4j
@Component
public class ConfirmCallbackService implements RabbitTemplate.ConfirmCallback {

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {

        // 打印异常信息
        if (!ack) {
            log.error("消息发送异常!correlationData={}, cause={}",correlationData.getId(),cause);
        }
    }
}
