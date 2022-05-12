import com.st.AppStarter;
import com.st.component.redis.RedisUtil;
import com.st.dao.GoodMapper;
import com.st.dao.OrderMapper;
import com.st.dao.ReceiptMapper;
import com.st.entity.po.Good;
import com.st.entity.po.Order;
import com.st.entity.po.Receipt;
import com.st.component.rmq.RMQSender;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(classes = AppStarter.class)
public class MyTest {

    @Autowired
    private GoodMapper goodMapper;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RMQSender rmqSender;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Test
    public void consistent(){
        BigDecimal[] prices = new BigDecimal[7];
        prices[1] = new BigDecimal("19.99");
        prices[2] = new BigDecimal("24.99");
        prices[3] = new BigDecimal("9.99");
        prices[4] = new BigDecimal("14.99");
        prices[5] = new BigDecimal("39.99");
        prices[6] = new BigDecimal("69.99");

        int[] setout = new int[7];

        List<Good> goods = goodMapper.selectList(null);
        List<Order> orders = orderMapper.selectList(null);
        List<Receipt> receipts = receiptMapper.selectList(null);

        BigDecimal sum1 = new BigDecimal("0");
        BigDecimal sum2 = new BigDecimal("0");

        boolean error = false;

        for (Order order:
             orders) {
            setout[order.getGid()] += order.getNumber();
            sum1 = sum1.add(new BigDecimal(order.getDiscount()).multiply(prices[order.getGid()]).multiply(new BigDecimal(order.getNumber())));
        }

        for (Receipt receipt:
             receipts) {
            sum2 = sum2.add(new BigDecimal(receipt.getMoney()));
        }
        for (Good g:
             goods) {
            System.out.println("预定库存："+g.getNumber()+" 实际售出："+setout[g.getGid()]+" 缓存剩余："+redisUtil.get("GC-"+g.getGid()));
            error = g.getNumber().equals(setout[g.getGid()]+g.getGid());
        }
        System.out.println("订单计算总金额："+sum1);
        System.out.println("收据计算总金额："+sum2);
        error = error && (sum1.equals(sum2));
        System.out.println("是否出现异常："+((error)?"是":"否"));
    }

}
