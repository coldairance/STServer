package com.st.socket.impl;

import com.st.common.entity.to.Message;
import com.st.socket.component.SocketSender;
import com.st.service.socket.SocketService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService
public class SocketImpl implements SocketService {

    @Autowired
    private SocketSender socketSender;

    @Override
    public void sendPublic(Message message) {
        socketSender.sendPublic(message);
    }
}
