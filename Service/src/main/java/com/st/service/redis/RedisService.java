package com.st.service.redis;


import java.util.Map;
import java.util.Set;

public interface RedisService {

    void clear();

    Object get(String key);
    void del(String key);
    boolean set(String key, Object obj);
    long increment(String key, long delta);
    long decrement(String key, long delta);

    void zsetIncrement(String key, Object o, Integer add);

    void zsetAdd(String key, Map<Object, Double> map);

    Set<Object> zsetRange(String key,int begin, int end);

    void hashIncrement(String key, String item, double by);

    Map<Object,Object> hashGetAll(String key);

    void expire(String key, long time);

    boolean lock(String key, long threadID);

    boolean lockWithDog(String key, long threadID);

    boolean unlock(String key, long threadID);
}
