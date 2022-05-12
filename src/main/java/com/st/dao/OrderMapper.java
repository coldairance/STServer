package com.st.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.st.entity.po.Order;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<Order> {

    @Update("truncate table st_order")
    void clear();
}
