package com.st.common.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Result implements Serializable {
    private int code;

    // 返回消息
    private String message;


    public Result(ResultCode orderRepetition) {
        this.code = orderRepetition.getCode();
        this.message = orderRepetition.getMessage();
    }

    public Result(ResultCode resultCode, String message){
        this.code = resultCode.getCode();
        this.message = message;
    }
}
