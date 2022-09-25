package com.st.common.entity.to;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class Message implements Serializable {
    private Integer uid;
    private Integer type;
    private Object payload;
}
