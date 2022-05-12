package com.st.controller;


import cn.hutool.http.server.HttpServerRequest;
import com.st.component.redis.RedisUtil;
import com.st.component.rmq.RMQReceiver;
import com.st.component.rmq.RMQSender;
import com.st.dao.GoodMapper;
import com.st.dao.OrderMapper;
import com.st.dao.ReceiptMapper;
import com.st.entity.po.Good;
import com.st.result.Result;
import com.st.result.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
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
    public Result reset(
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
        String goodRank = "GR-";
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
            rank.put(g,new Double(0));
        });

        // 初始化排行榜
        redisUtil.zAdd("rank",rank);

        return new Result(ResultCode.SUCCESS);
    }
}
