package com.st.interceptor;

import com.st.component.redis.RedisLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class fluxInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisLimiter redisLimiter;

    @Value("${myself.limit.entrance.time}")
    private Integer limitTime;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if(redisLimiter.limit("entrance", limitTime, 1)){
            return HandlerInterceptor.super.preHandle(request, response, handler);
        }
        System.out.println("拦截...");
        return false;
    }
}
