package com.st.entity.to;

import com.st.entity.po.Order;
import com.st.entity.po.Receipt;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class Store implements Serializable {

    private Order order;
    private Receipt receipt;
}
