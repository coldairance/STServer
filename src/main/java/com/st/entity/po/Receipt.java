package com.st.entity.po;

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

    @TableId(type = IdType.AUTO)
    private Integer rid;
    private Integer uid;
    private Integer oid;
    private String money;
}
