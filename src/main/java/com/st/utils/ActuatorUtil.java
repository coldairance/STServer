package com.st.utils;

import cn.hutool.http.HttpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ActuatorUtil {
    @Value("${server.port}")
    private int port;


    public  Map<String,String> getUsedMemory(){
        String PRE = "localhost:"+port+"/actuator/metrics/jvm.memory.used?";
        Map<String,String> map = new HashMap<>();
        map.put("eden",HttpUtil.get(PRE+"tag=area:heap&tag=id:PS Eden Space"));
        map.put("survivor",HttpUtil.get(PRE+"tag=area:heap&tag=id:PS Survivor Space"));
        map.put("old",HttpUtil.get(PRE+"tag=area:heap&tag=id:PS Old Gen"));
        map.put("nonheap",HttpUtil.get(PRE+"tag=area:nonheap"));
        return map;
    }

    public  Map<String,String> getCommittedMemory(){
        String PRE = "localhost:"+port+"/actuator/metrics/jvm.memory.committed?";
        Map<String,String> map = new HashMap<>();
        map.put("eden",HttpUtil.get(PRE+"tag=area:heap&tag=id:PS Eden Space"));
        map.put("survivor",HttpUtil.get(PRE+"tag=area:heap&tag=id:PS Survivor Space"));
        map.put("old",HttpUtil.get(PRE+"tag=area:heap&tag=id:PS Old Gen"));
        map.put("nonheap",HttpUtil.get(PRE+"tag=area:nonheap"));
        return map;
    }

    public Map<String,String> getGCInfo(){
        String PRE = "localhost:"+port+"/actuator/metrics/jvm.gc.pause?";
        Map<String,String> map = new HashMap<>();
        map.put("minor",HttpUtil.get(PRE+"tag=action:end of minor GC"));
        map.put("major",HttpUtil.get(PRE+"tag=action:end of major GC"));
        return map;
    }
}
