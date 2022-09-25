package com.st.gateway.controller;
import cn.hutool.http.server.HttpServerRequest;
import com.st.common.entity.po.Good;
import com.st.common.entity.po.Order;
import com.st.common.entity.po.Receipt;
import com.st.common.utils.Result;
import com.st.common.utils.ResultCode;
import com.st.service.mysql.MysqlService;
import com.st.service.redis.RedisService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/")
public class OtherController {

    @DubboReference
    private RedisService redisService;

    @DubboReference
    private MysqlService mysqlService;

    @GetMapping("get/all")
    public List<Good> getInfo(
            HttpServerRequest request,
            HttpServletResponse response
    ) {
        List<Good> goods = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Good good = (Good) redisService.get("GI-" + i);
            goods.add(good);
        }

        return goods;
    }

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
        redisService.clear();

        // 清空表
        mysqlService.clearOrders();
        mysqlService.clearReceipts();

        // 恢复库存
        mysqlService.restore(store);

        // 加载商品信息
        List<Good> goods = mysqlService.getGoods();
        goods.forEach(g -> {
            redisService.set(goodInfo+g.getGid(),g);
            redisService.set(goodCount+g.getGid(),g.getNumber());
            // 0.85
            redisService.set(goodDiscount+g.getGid(),g.getNumber()/10);
            rank.put(g.getGid(), new Double(0));
        });

        // 初始化排行榜
        redisService.zsetAdd("rank",rank);
        // 服务ID
        redisService.set("machineID",0);

        return new Result(ResultCode.SUCCESS);
    }

    @GetMapping("instances")
    public Object instances(
            HttpServerRequest request,
            HttpServletResponse response
    ) {
        Map<Object, Object> order = redisService.hashGetAll("order-instances");
        Map<Object, Object> pay = redisService.hashGetAll("pay-instances");
        HashMap<String, Object> map = new HashMap<>();
        map.put("order",order);
        map.put("pay", pay);
        return map;
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

        List<Good> goods = mysqlService.getGoods();
        List<Order> orders = mysqlService.getOrders();
        List<Receipt> receipts = mysqlService.getReceipts();

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
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("商品名：", g.getName());
            map.put("预订：", "库存：" + g.getNumber() + " 优惠券：" + g.getNumber() / 10);
            map.put("实际售出：", "库存：" + setout[g.getGid()] + " 优惠券：" + discount[g.getGid()]);
            map.put("缓存剩余：", "库存：" + redisService.get("GC-" + g.getGid()) + " 优惠券：" + redisService.get("GD-" + g.getGid()));
            conditions.add(map);
            int goodCnt = g.getNumber() - (setout[g.getGid()] + (Integer) redisService.get("GC-" + g.getGid()));
            int disCnt = (g.getNumber()/10) - (discount[g.getGid()] + (Integer) redisService.get("GD-" + g.getGid()));
            if(goodCnt!=0){
                map.put("库存：","异常");
            } else{
                map.put("库存：","正常");
            }

            if(disCnt!=0){
                map.put("折扣：","异常");
            }else{
                map.put("折扣：","正常");
            }
            error |= (goodCnt!=0);
            error |= (disCnt!=0);
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
