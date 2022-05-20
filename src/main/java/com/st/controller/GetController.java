package com.st.controller;

import cn.hutool.http.server.HttpServerRequest;
import com.st.entity.po.Good;
import com.st.component.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/get")
public class GetController {


    @Autowired
    private RedisUtil redisUtil;

    @GetMapping("all")
    public List<Good> getInfo(
            HttpServerRequest request,
            HttpServletResponse response
    ) {
        List<Good> goods = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Good good = (Good) redisUtil.get("GI-" + i);
            goods.add(good);
        }

        return goods;
    }
}
