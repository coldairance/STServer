package com.st.order.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class Context {

    private long datacenterId;

    private long machineId;
}
