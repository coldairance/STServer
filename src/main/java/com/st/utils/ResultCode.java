package com.st.utils;

public enum ResultCode {

    SUCCESS(200, "成功"),
    ERROR(500,"失败"),
    ORDER_REPETITION(501, "订单重复"),
    ORDER_EXPIRED(502,"订单过期"),
    PAY_REPETITION(503,"重复购买"),
    GOOD_EMPTY(504,"商品库存为空"),
    SERVER_BUSY(505, "服务繁忙");


    private Integer code;

    private String message;

    ResultCode(Integer code, String message){
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
