package com.st.service.mysql;

import com.st.common.entity.po.Good;
import com.st.common.entity.po.Order;
import com.st.common.entity.po.Receipt;

import java.sql.SQLException;
import java.util.List;

public interface MysqlService {

    void restore(int num);
    List<Good> getGoods();
    void clearOrders();
    void store(Order order, Receipt receipt) throws RuntimeException;
    List<Order> getOrders();
    void clearReceipts();
    List<Receipt> getReceipts();
}
