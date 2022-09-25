package com.st.redis.impl;

import com.st.redis.component.RedisLock;
import com.st.redis.component.RedisUtil;
import com.st.service.redis.RedisService;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@DubboService
public class RedisImpl implements RedisService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisLock redisLock;

    @Override
    public void clear() {
        redisUtil.clear();
    }

    @Override
    public Object get(String key) {
        return redisUtil.get(key);
    }

    @Override
    public void del(String key) {
        redisUtil.del(key);
    }


    @Override
    public boolean set(String key, Object obj) {
        return redisUtil.set(key, obj);
    }

    @Override
    public long increment(String key, long delta) {
        return redisUtil.incr(key,delta);
    }

    @Override
    public long decrement(String key, long delta) {
        return redisUtil.decr(key,delta);
    }


    @Override
    public void zsetIncrement(String key, Object o, Integer add) {
        redisUtil.zIncrement(key,o,add);
    }

    @Override
    public void zsetAdd(String key, Map<Object, Double> map) {
        redisUtil.zAdd(key,map);
    }

    @Override
    public Set<Object> zsetRange(String key, int begin, int end) {
        return redisUtil.zRange(key,begin,end);
    }

    @Override
    public void hashIncrement(String key, String item, double by) {
        redisUtil.hincr(key, item, by);
    }

    @Override
    public Map<Object, Object> hashGetAll(String key) {
        return redisUtil.hmget(key);
    }

    @Override
    public void expire(String key, long time) {
        redisUtil.expire(key,time);
    }

    @Override
    public boolean lock(String key, long threadID) {
//        while(!redisLock.lock(key, threadID,10));
//        return true;
        RLock lock = redissonClient.getLock(key);
        try{
            boolean b = lock.tryLock(30L, 180L, TimeUnit.SECONDS);
            if(b){
                lock.unlock();
            }
        }catch (InterruptedException e){

        }
        return true;
    }

    @Override
    public boolean lockWithDog(String key, long threadID) {
        while(!redisLock.lockWithDog(key, threadID,10));
        return true;
    }

    @Override
    public boolean unlock(String key, long threadID) {
        return redisLock.unlock(key, threadID);
    }
}
