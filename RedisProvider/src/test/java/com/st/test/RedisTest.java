package com.st.test;

import com.st.redis.RedisProviderApplication;
import com.st.redis.component.RedisLock;
import com.st.redis.component.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@SpringBootTest(classes = RedisProviderApplication.class)
@Slf4j
public class RedisTest {

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private ScheduledThreadPoolExecutor pool;

    @Autowired
    private RedisUtil redisUtil;


    @Test
    public void testLock(){
        redisLock.lock("test",1,10);
    }

    @Test
    public void testDog() throws InterruptedException {
        redisLock.lockWithDog("test",111,30);
        Thread.sleep(4000);
        redisLock.unlock("test",111);
        for (int i = 0; i < 10; i++) {
            System.out.println(pool.getQueue().size());
            Thread.sleep(10000);
        }
    }
}
