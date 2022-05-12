package com.st.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.st.entity.po.Good;
import org.apache.ibatis.annotations.Update;

public interface GoodMapper extends BaseMapper<Good> {

    @Update("update st_good set number=#{number}")
    void restore(Integer number);
}
