package com.st.order.config;

import com.st.common.component.SnowFlake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class Config {

    @Bean
    public SnowFlake injectSnowFlake(){
        return new SnowFlake();
    }
}
