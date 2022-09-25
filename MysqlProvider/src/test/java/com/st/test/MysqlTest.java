package com.st.test;

import com.st.common.entity.po.Order;
import com.st.common.entity.po.Receipt;
import com.st.mysql.MySqlProviderApplication;
import com.st.mysql.mapper.OrderMapper;
import com.st.mysql.mapper.ReceiptMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = MySqlProviderApplication.class)
public class MysqlTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Test
    public void testMysql(){
        Receipt receipt = new Receipt();
        receipt.setOid(738497249587400704l);
        receipt.setRid(738497249587400730l);
        receipt.setMoney("80.3");
        receipt.setUid(1100);
        receiptMapper.insert(receipt);
    }
}
