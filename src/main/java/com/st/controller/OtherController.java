package com.st.controller;


import cn.hutool.http.server.HttpServerRequest;
import com.st.component.redis.RedisUtil;
import com.st.component.rmq.RMQReceiver;
import com.st.component.rmq.RMQSender;
import com.st.dao.GoodMapper;
import com.st.dao.OrderMapper;
import com.st.dao.ReceiptMapper;
import com.st.entity.po.Good;
import com.st.entity.po.Order;
import com.st.entity.po.Receipt;
import com.st.utils.Result;
import com.st.utils.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OtherController {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private GoodMapper goodMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private RMQSender rmqSender;

    @Autowired
    private RMQReceiver rmqReceiver;



    @GetMapping("reset")
    public Object reset(
            HttpServerRequest request,
            HttpServletResponse response,
            @RequestParam Integer store
    ){
        // 前缀
        // 商品信息
        String goodInfo = "GI-";
        // 商品库存
        String goodCount = "GC-";
        // 商品销售排行
        String goodRank = "rank";
        Map<Object,Double> rank = new HashMap<>();
        // 商品折扣
        String goodDiscount = "GD-";
        // 订单信息
        String goodOrder = "GO-";

        // 清空缓存
        redisUtil.clear();

        // 清空表
        orderMapper.clear();
        receiptMapper.clear();

        // 恢复库存
        goodMapper.restore(store);

        // 加载商品信息
        List<Good> goods = goodMapper.selectList(null);
        goods.forEach(g -> {
            redisUtil.set(goodInfo+g.getGid(),g);
            redisUtil.set(goodCount+g.getGid(),g.getNumber());
            // 0.85
            redisUtil.set(goodDiscount+g.getGid(),g.getNumber()/10);
            rank.put(g.getGid(), new Double(0));
        });

        // 初始化排行榜
        redisUtil.zAdd("rank",rank);

        return new Result(ResultCode.SUCCESS);
    }


    @GetMapping("condition")
    public Object condition(
            HttpServerRequest request,
            HttpServletResponse response
    ) {
        List<Map<String,Object>> conditions = new ArrayList<>();

        BigDecimal[] prices = new BigDecimal[7];
        prices[1] = new BigDecimal("19.99");
        prices[2] = new BigDecimal("24.99");
        prices[3] = new BigDecimal("49.99");
        prices[4] = new BigDecimal("24.99");
        prices[5] = new BigDecimal("39.99");
        prices[6] = new BigDecimal("29.99");

        int[] setout = new int[7];
        int[] discount = new int[7];

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
            if(order.getDiscount().equals("0.85")) discount[order.getGid()]++;
        }

        for (Receipt receipt:
                receipts) {
            sum2 = sum2.add(new BigDecimal(receipt.getMoney()));
        }
        for (Good g:
                goods) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("商品名：", g.getName());
            map.put("预订：", "库存：" + g.getNumber() + " 优惠券：" + g.getNumber() / 10);
            map.put("实际售出：", "库存：" + setout[g.getGid()] + " 优惠券：" + discount[g.getGid()]);
            map.put("缓存剩余：", "库存：" + redisUtil.get("GC-" + g.getGid()) + " 优惠券：" + redisUtil.get("GD-" + g.getGid()));
            conditions.add(map);
            error |= !g.getNumber().equals(setout[g.getGid()] + (Integer) redisUtil.get("GC-" + g.getGid()));
            error |= !((g.getNumber() / 10) == (discount[g.getGid()] + (Integer) redisUtil.get("GD-" + g.getGid())));
        }
        error |= (sum1.equals(sum2)) && error;
        HashMap<String, Object> map = new HashMap<>();
        map.put("订单计算总金额：",sum1);
        map.put("收据计算总金额：",sum2);
        map.put("是否出现异常：",((error)?"是":"否"));

        conditions.add(map);
        return conditions;
    }
}
