package com.st.gateway.controller;
import cn.hutool.http.server.HttpServerRequest;
import com.st.common.utils.Result;
import com.st.service.order.PlaceOrderService;
import com.st.service.pay.PayOrderService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/v3")
public class Entrance {

    @DubboReference
    private PlaceOrderService orderService;

    @DubboReference
    private PayOrderService payService;

    @PostMapping("order")
    public Result addOrder(
            HttpServerRequest request,
            HttpServletResponse response,
            @RequestBody String data
    ) throws Exception {
        return orderService.order(data);
    }


    @PostMapping("pay")
    public Result addReceiptMapper(
            HttpServerRequest request,
            HttpServletResponse response,
            @RequestBody String data
    ){
        return payService.pay(data);
    }
}
