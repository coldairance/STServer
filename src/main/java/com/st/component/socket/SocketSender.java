package com.st.component.socket;

import cn.hutool.json.JSONUtil;
import com.st.entity.to.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SocketSender {

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

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
