package com.st.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@TableName("st_good")
@ToString
public class Good implements Serializable {

    @TableId
    private Integer gid;
    private String name;
    private String price;
    private Integer number;
}
