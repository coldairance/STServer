package com.st.utils;

import lombok.Data;

@Data
public class Result {
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
