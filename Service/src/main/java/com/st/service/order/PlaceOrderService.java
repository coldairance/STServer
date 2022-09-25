package com.st.service.order;

import com.st.common.utils.Result;

public interface PlaceOrderService {
    Result order(String data) throws Exception;
}
