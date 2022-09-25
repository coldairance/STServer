package com.st.rabbitmq.impl;

import com.st.common.entity.po.Order;
import com.st.common.entity.po.Receipt;
import com.st.rabbitmq.component.RMQSender;
import com.st.service.rabbit.RabbitService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService
public class RabbitImpl implements RabbitService {

    @Autowired
    private RMQSender rmqSender;

    @Override
    public void sendDBOrder(Order order, Receipt receipt) {
        rmqSender.sendDBOrder(order,receipt);
    }

    @Override
    public void sendExpiredOrder(Order order) {
        rmqSender.sendExpiredOrder(order);
    }
}
