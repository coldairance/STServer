package com.st.order.config;

import com.st.common.component.SnowFlake;
import com.st.service.redis.RedisService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Post implements ApplicationRunner {

    @DubboReference
    private RedisService redisService;

    @Autowired
    private Context context;

    @Autowired
    private SnowFlake snowFlake;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        context.setDatacenterId(0);
        context.setMachineId(redisService.increment("machineID",1));
        snowFlake.setWorkerId(context.getMachineId());
        snowFlake.setDatacenterId(context.getDatacenterId());
    }
}
