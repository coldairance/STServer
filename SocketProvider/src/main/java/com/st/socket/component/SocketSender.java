package com.st.socket.component;

import cn.hutool.json.JSONUtil;
import com.st.common.entity.to.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SocketSender {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 向所有客户端广播消息
     * @param
     */
    //@SendTo("/topic/public_queue")
    public void sendPublic(Message message){
        simpMessagingTemplate.convertAndSend("/topic/public_queue", JSONUtil.toJsonStr(message));
    }
}
