package com.st.result;

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
}
