package com.st.redis.component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisLock {

    class WatchDog implements Runnable{
        private List<String> keys;
        private long threadID;
        private long ttl;

        private ScheduledFuture current;

        public WatchDog(List<String> keys, long threadID,long ttl){
            this.keys = keys;
            this.threadID = threadID;
            this.ttl = ttl;
        }

        @Override
        public void run() {
            try{
                RedisScript<Integer> redisScript = new DefaultRedisScript<>(dogScript, Integer.class);
                Integer ret = (Integer) redisTemplate.execute(redisScript, keys, threadID, ttl);
                if(ret==0){
                    throw new Exception();
                }
            } catch (Exception e) {
                current.cancel(true);
            }
        }
    }

    private String lockScript;
    private String unlockScript;

    private String dogScript;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ScheduledThreadPoolExecutor pool;

    public RedisLock(){
        this.lockScript = buildLockScript();
        this.unlockScript = buildUnlockScript();
        this.dogScript = buildDogScript();
    }

    public boolean lock(String key,long threadID,int expired){
        List<String> keys = Collections.singletonList(key);
        RedisScript<Integer> redisScript = new DefaultRedisScript<>(lockScript, Integer.class);
        Integer ret = (Integer) redisTemplate.execute(redisScript, keys, threadID, expired);
        return ret!=0;
    }

    public boolean lockWithDog(String key,long threadID,int expired){
        List<String> keys = Collections.singletonList(key);
        RedisScript<Integer> redisScript = new DefaultRedisScript<>(lockScript, Integer.class);
        Integer ret = (Integer) redisTemplate.execute(redisScript, keys, threadID, expired);
        if(ret==0){
            return false;
        }else{
            WatchDog watchDog = new WatchDog(keys, threadID, expired);
            ScheduledFuture<?> scheduledFuture = pool.scheduleAtFixedRate(watchDog, 1, expired / 3, TimeUnit.SECONDS);
            watchDog.current = scheduledFuture;
            return true;
        }
    }

    public boolean unlock(String key,long threadID){
        List<String> keys = Collections.singletonList(key);
        DefaultRedisScript<Integer> redisScript = new DefaultRedisScript<>(unlockScript, Integer.class);
        Integer ret = (Integer)redisTemplate.execute(redisScript, keys, threadID);
        return ret!=0;
    }

    /**
     * if redis.call('setnx',KEYS[1],ARGV[1]) == 1 then
     *      return tostring(redis.call('expire',KEYS[1],ARGV[2]))
     * else
     *      return tostring(0)
     * end
     */
    private String buildLockScript() {
        StringBuilder lua = new StringBuilder();
        lua.append("if redis.call('setnx',KEYS[1],ARGV[1]) == 1 then\n")
                .append("return tostring(redis.call('expire',KEYS[1],ARGV[2]))\n")
                .append("else\n")
                .append("return tostring(0)\n")
                .append("end");
        return lua.toString();
    }

    /**
     * if redis.call('get',KEYS[1]) == ARGV[1] then
     *      return tostring(redis.call('del',KEYS[1]))
     * else
     *      return tostring(0)
     * end
     */
    private String buildUnlockScript() {
        StringBuilder lua = new StringBuilder();
        lua.append("if redis.call('get',KEYS[1]) == ARGV[1] then\n")
                .append("return tostring(redis.call('del',KEYS[1]))\n")
                .append("else\n")
                .append("return tostring(0)\n")
                .append("end");
        return lua.toString();
    }

    /**
     * if redis.call('get',KEYS[1]) == ARGV[1] then
     *      redis.call('expire',KEYS[1],ARGV[2])
     *      return tostring(1)
     * else
     *      return tostring(0)
     * end
     */
    private String buildDogScript(){
        StringBuilder lua = new StringBuilder();
        lua.append("if redis.call('get',KEYS[1]) == ARGV[1] then\n")
                .append("redis.call('expire',KEYS[1],ARGV[2])\n")
                .append("return tostring(1)\n")
                .append("else\n")
                .append("return tostring(0)\n")
                .append("end");
        return lua.toString();
    }
}
