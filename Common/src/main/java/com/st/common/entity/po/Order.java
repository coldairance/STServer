package com.st.common.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
@TableName("st_order")
public class Order implements Serializable {

    @TableId(value = "oid", type = IdType.INPUT)
    private Long oid;
    private Integer uid;
    private Integer gid;
    private Integer number;
    private String discount;
}
