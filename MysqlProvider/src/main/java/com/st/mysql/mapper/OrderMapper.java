package com.st.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.st.common.entity.po.Order;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<Order> {

    @Update("truncate table st_order")
    void clear();
}
