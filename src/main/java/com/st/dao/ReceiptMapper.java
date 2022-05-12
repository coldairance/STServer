package com.st.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.st.entity.po.Receipt;
import org.apache.ibatis.annotations.Update;

public interface ReceiptMapper extends BaseMapper<Receipt> {

    @Update("truncate table st_receipt")
    void clear();
}
