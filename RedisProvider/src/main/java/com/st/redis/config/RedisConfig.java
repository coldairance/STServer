package com.st.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.ScheduledThreadPoolExecutor;


@Configuration
public class RedisConfig {

    /**
     * redis 地址
     */
    @Value(value = "${myself.redisson.address}")
    private String address;
    /**
     * redis 数据库编号
     */
    @Value(value = "${myself.redisson.database}")
    private Integer database;
    /**
     * redis 最小连接数量
     */
    @Value(value = "${myself.redisson.connectionMinimumIdleSize}")
    private Integer connectionMinimumIdleSize;
    /**
     * redis 最大连接池大小
     */
    @Value(value = "${myself.redisson.connectionPoolSize}")
    private Integer connectionPoolSize;
    /**
     * redis 连接超时时间(毫秒)
     */
    @Value(value = "${myself.redisson.connectionTimeout}")
    private Integer connectionTimeout;
    /**
     * redis 服务器响应时间(毫秒)
     */
    @Value(value = "${myself.redisson.timeout}")
    private Integer timeout;


    /**
     * 创建 Redisson 客户端
     *
     * @return
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        /**
         * 运行模式
         * useSingleServer: 单机模式
         * useMasterSlaveServers: 主从模式
         * useSentinelServers: 哨兵模式
         */
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectTimeout(connectionTimeout)
                .setTimeout(timeout);
        return Redisson.create(config);

    }


    // 定时线程池
    @Bean
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor(){
        return new ScheduledThreadPoolExecutor(200);
    }

    // 方法模板
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(factory);
        // key采用String的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        // value序列化方式采用jackson
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存注解
     * @param redisConnectionFactory
     * @return
     */
    @Bean(name = "redisCacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        //初始化一个RedisCacheWriter
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
        //设置CacheManager的值序列化方式为json序列化
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                // 序列化
                .serializeValuesWith(pair);

        //初始化RedisCacheManager
        return new RedisCacheManager(redisCacheWriter, defaultCacheConfig);
    }


}
