package com.st.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.st.common.entity.po.Receipt;
import org.apache.ibatis.annotations.Update;

public interface ReceiptMapper extends BaseMapper<Receipt> {
    @Update("truncate table st_receipt")
    void clear();
}
