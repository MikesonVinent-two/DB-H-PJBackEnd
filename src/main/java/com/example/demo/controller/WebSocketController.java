package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.example.demo.dto.WebSocketMessage;
import com.example.demo.service.WebSocketService;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket控制器，处理WebSocket连接和消息
 */
@Controller
public class WebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    
    private final WebSocketService webSocketService;
    
    @Autowired
    public WebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }
    
    /**
     * 处理客户端发送的批次订阅确认消息
     */
    @MessageMapping("/batch/{batchId}/subscribe")
    @SendTo("/topic/batch/{batchId}")
    public WebSocketMessage confirmBatchSubscription(@DestinationVariable Long batchId) {
        logger.debug("客户端订阅批次消息: {}", batchId);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", batchId);
        payload.put("subscribed", true);
        payload.put("message", "成功订阅批次消息");
        payload.put("source", "system");
        
        return new WebSocketMessage(WebSocketMessage.MessageType.STATUS_CHANGE, payload);
    }
    
    /**
     * 处理客户端发送的运行订阅确认消息
     */
    @MessageMapping("/run/{runId}/subscribe")
    @SendTo("/topic/run/{runId}")
    public WebSocketMessage confirmRunSubscription(@DestinationVariable Long runId) {
        logger.debug("客户端订阅运行消息: {}", runId);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", runId);
        payload.put("subscribed", true);
        payload.put("message", "成功订阅运行消息");
        payload.put("source", "system");
        
        return new WebSocketMessage(WebSocketMessage.MessageType.STATUS_CHANGE, payload);
    }
    
    /**
     * 处理客户端发送的全局订阅确认消息
     */
    @MessageMapping("/global/subscribe")
    @SendTo("/topic/global")
    public WebSocketMessage confirmGlobalSubscription() {
        logger.debug("客户端订阅全局消息");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("subscribed", true);
        payload.put("message", "成功订阅全局消息");
        payload.put("source", "system");
        
        return new WebSocketMessage(WebSocketMessage.MessageType.STATUS_CHANGE, payload);
    }
} 