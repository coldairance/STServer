package com.st.aspect;

import com.rabbitmq.client.Channel;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class RabbitListenerAspect {

    @Pointcut("execution(* com.st.component.rmq.RMQReceiver.*(..))")
    public void returnPointCut() {

    }

    @Around("returnPointCut()")
    public void Around(ProceedingJoinPoint point) throws Throwable {
        point.proceed();
        Object[] args = point.getArgs();
        Channel channel = (Channel) args[0];
        Message message = (Message) args[1];
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
