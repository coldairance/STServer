package com.st.service.rabbit;

import com.st.common.entity.po.Order;
import com.st.common.entity.po.Receipt;

public interface RabbitService{

    void sendDBOrder(Order order, Receipt receipt);

    void sendExpiredOrder(Order order);
}
