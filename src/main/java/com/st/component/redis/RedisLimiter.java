package com.st.component.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisLimiter {

    @Autowired
    private RedisTemplate redisTemplate;

    private String luaScript;

    public RedisLimiter(){
        this.luaScript = buildLuaScript();
    }

    /**
     * 达到限流时则等待，直到新的间隔。
     */
    public void limitWait(String key, int limitCount, int limitSecond) throws InterruptedException {
        boolean ok;//放行标志
        do {
            ok = limit(key, limitCount, limitSecond);//调用limit返回是否达到限流
            if (!ok) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
                if (null != ttl && ttl > 0) {
                    Thread.sleep(ttl);//睡眠到缓存过期
                }
            }
        } while (!ok);
    }

    /**
     * 限流方法 true-放行；false-限流
     * key:缓存的key,limitCount:key的最大值,limitSecond:key的过期时间
     */
    public boolean limit(String key, int limitCount, int limitSecond) {
        List<String> keys = Collections.singletonList(key);
        RedisScript<Integer> redisScript = new DefaultRedisScript<>(luaScript, Integer.class);
        Integer count = (Integer) redisTemplate.execute(redisScript, keys, limitCount, limitSecond);
        if (count != null && count.intValue() <= limitCount) {//判断是否超过数量
            return true;//放行
        } else {
            return false;//限流
        }
    }

    /**
     * 编写redis Lua 限流脚本
     * 流程：如果超过指定数量就返回，没有超过就计数+1，返回当前的值。第一次进来的时候还要给缓存设置过期时间。
     *  c = redis.call('get',KEYS[1]) //从缓存中获取KEY
     *  if c and tonumber(c) > tonumber(ARGV[1]) then  return c end //超过限定的最大值就直接返回
     *  c = redis.call('incr',KEYS[1])    //没有超过计数器+1,返回当前c的值
     *  if tonumber(c) == 1 then redis.call('expire',KEYS[1],ARGV[2]) end
     *  return c;
     *  注:Incr命令将key中储存的数字值增一，返回当前key的数值。
     *  注:tonumber方法是Lua语法里面的函数，把参数转成10进制的数字
     *  注:KEYS[1] 表示key，ARGV[1]表示:value,ARGV[2]:超时时间
     */
    private String buildLuaScript() {
        StringBuilder lua = new StringBuilder();
        lua.append("local c");
        lua.append("\nc = redis.call('get',KEYS[1])");
        lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then");//实际调用次数超过阈值，则直接返回
        lua.append("\nreturn tostring(c);");
        lua.append("\nend");
        lua.append("\nc = redis.call('incr',KEYS[1])");// 执行计算器自加
        lua.append("\nif tonumber(c) == 1 then");
        lua.append("\nredis.call('expire',KEYS[1],ARGV[2])");//从第一次调用开始限流，设置对应键值的过期
        lua.append("\nend");
        lua.append("\nreturn tostring(c);");
        return lua.toString();
    }
}
