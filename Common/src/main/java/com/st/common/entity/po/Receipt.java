package com.st.common.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;


@Data
@ToString
@TableName("st_receipt")
public class Receipt implements Serializable {
    @TableId(value = "rid",type = IdType.INPUT)
    private Long rid;
    private Long oid;
    private Integer uid;
    private String money;
}
