package com.st.controller;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.json.JSONUtil;
import com.st.utils.ActuatorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
@RequestMapping("config")
public class ConfigController {

    private static final String TOMCAT_THREADS_MAX="server.tomcat.threads.max";
    private static final String HIKARI_MAX_SIZE="spring.datasource.hikari.maximum-pool-size";
    private static final String RABBIT_MQ_MAX_CONCURRENCY="spring.rabbitmq.listener.simple.max-concurrency";
    private static final String MYSELF_LIMIT_USER="myself.limit.user.time";
    private static final String MYSELF_LIMIT_ENTRANCE="myself.limit.entrance.time";
    private static final String REDIS_KEY_EXPIRED="myself.expired";

    private static final String JVM_START_SH="myself.jvmstart";

    @Value("${"+TOMCAT_THREADS_MAX+"}")
    private int tomcat_threads_max;

    @Value("${"+HIKARI_MAX_SIZE+"}")
    private int hikari_max_size;

    @Value("${"+RABBIT_MQ_MAX_CONCURRENCY+"}")
    private int rabbit_mq_max_concurrency;

    @Value("${"+MYSELF_LIMIT_USER+"}")
    private int myself_limit_user;

    @Value("${"+MYSELF_LIMIT_ENTRANCE+"}")
    private int myself_limit_entrance;

    @Value("${"+REDIS_KEY_EXPIRED+"}")
    private int redis_key_expired;

    @Value("${"+JVM_START_SH+"}")
    private String jvm_start_sh;

    @Autowired
    private ActuatorUtil actuatorUtil;

    @GetMapping("jvm/runtime")
    public String usedMemory(){
        Map<String,Object> ret = new HashMap<>();
        ret.put("used",actuatorUtil.getUsedMemory());
        ret.put("committed", actuatorUtil.getCommittedMemory());
        ret.put("gc",actuatorUtil.getGCInfo());

        return JSONUtil.toJsonStr(ret);
    }

    @GetMapping("properties")
    public Map config() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(TOMCAT_THREADS_MAX, tomcat_threads_max);
        map.put(HIKARI_MAX_SIZE, hikari_max_size);
        map.put(RABBIT_MQ_MAX_CONCURRENCY, rabbit_mq_max_concurrency);
        map.put(MYSELF_LIMIT_USER, myself_limit_user);
        map.put(MYSELF_LIMIT_ENTRANCE, myself_limit_entrance);
        map.put(REDIS_KEY_EXPIRED, redis_key_expired);

        List<String> jvmOptions = new ArrayList<>();
        FileReader fileReader = new FileReader(jvm_start_sh);
        String result = fileReader.readString();
        String pattern = " -X.+ ";
        Matcher matcher = Pattern.compile(pattern).matcher(result);
        while (matcher.find()){
            jvmOptions.add(matcher.group().trim());
        }
        map.put("jvm.options",jvmOptions);
        return map;
    }
}
