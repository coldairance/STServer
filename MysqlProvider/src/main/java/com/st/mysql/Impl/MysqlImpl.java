package com.st.mysql.Impl;

import com.st.common.entity.po.Good;
import com.st.common.entity.po.Order;
import com.st.common.entity.po.Receipt;
import com.st.mysql.mapper.GoodMapper;
import com.st.mysql.mapper.OrderMapper;
import com.st.mysql.mapper.ReceiptMapper;
import com.st.service.mysql.MysqlService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@DubboService
public class MysqlImpl implements MysqlService {

    @Autowired
    private GoodMapper goodMapper;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private OrderMapper orderMapper;


    @Override
    public void restore(int num) {
        goodMapper.restore(num);
    }

    @Override
    public List<Good> getGoods() {
        return goodMapper.selectList(null);
    }

    @Override
    public void clearOrders() {
        orderMapper.clear();
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void store(Order order, Receipt receipt) {
        orderMapper.insert(order);
        receiptMapper.insert(receipt);
    }


    @Override
    public List<Order> getOrders() {
        return orderMapper.selectList(null);
    }

    @Override
    public void clearReceipts() {
        receiptMapper.clear();
    }


    @Override
    public List<Receipt> getReceipts() {
        return receiptMapper.selectList(null);
    }
}
