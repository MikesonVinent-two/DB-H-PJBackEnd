package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.WebSocketMessage;
import com.example.demo.dto.WebSocketMessage.MessageType;

/**
 * WebSocket服务实现类，用于向客户端发送消息
 */
@Service
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * 发送批次相关消息
     * 
     * @param batchId 批次ID
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendBatchMessage(Long batchId, MessageType type, Map<String, Object> payload) {
        String destination = "/topic/batch/" + batchId;
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送运行相关消息
     * 
     * @param runId 运行ID
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendRunMessage(Long runId, MessageType type, Map<String, Object> payload) {
        String destination = "/topic/run/" + runId;
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送用户相关消息
     * 
     * @param userId 用户ID
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendUserMessage(Long userId, MessageType type, Map<String, Object> payload) {
        String destination = "/user/" + userId + "/queue/messages";
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送全局消息
     * 
     * @param type 消息类型
     * @param payload 消息内容
     */
    public void sendGlobalMessage(MessageType type, Map<String, Object> payload) {
        String destination = "/topic/global";
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送状态变更消息
     * 
     * @param entityId 实体ID
     * @param status 状态
     * @param message 消息内容
     */
    public void sendStatusChangeMessage(Long entityId, String status, String message) {
        String destination = "/topic/status/" + entityId;
        WebSocketMessage wsMessage = new WebSocketMessage(MessageType.STATUS_CHANGE, 
                Map.of("id", entityId, "status", status, "message", message));
        messagingTemplate.convertAndSend(destination, wsMessage);
    }
    
    /**
     * 发送运行进度消息
     * 
     * @param runId 运行ID
     * @param progress 进度百分比
     * @param message 消息内容
     */
    public void sendRunProgressMessage(Long runId, double progress, String message) {
        String destination = "/topic/progress/run/" + runId;
        WebSocketMessage wsMessage = new WebSocketMessage(MessageType.PROGRESS_UPDATE, 
                Map.of("runId", runId, "progress", progress, "message", message));
        messagingTemplate.convertAndSend(destination, wsMessage);
    }
    
    /**
     * 发送错误消息
     *
     * @param entityId 实体ID（批次ID或运行ID）
     * @param errorMessage 错误消息
     * @param status 当前状态（可选）
     */
    public void sendErrorMessage(Long entityId, String errorMessage, Object status) {
        String destination = "/topic/error/" + entityId;
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", entityId);
        payload.put("error", errorMessage);
        payload.put("timestamp", System.currentTimeMillis());
        
        // 如果提供了状态，也添加进去
        if (status != null) {
            payload.put("status", status.toString());
        }
        
        WebSocketMessage wsMessage = new WebSocketMessage(MessageType.ERROR, payload);
        messagingTemplate.convertAndSend(destination, wsMessage);
        
        // 同时发送到全局错误频道
        messagingTemplate.convertAndSend("/topic/errors", wsMessage);
    }
    
    /**
     * 发送所有批次状态更新
     * 
     * @param batches 批次状态列表
     */
    public void sendAllBatchesStatus(List<AnswerGenerationBatchDTO> batches) {
        String destination = "/topic/batches/all";
        Map<String, Object> payload = new HashMap<>();
        payload.put("batches", batches);
        payload.put("timestamp", System.currentTimeMillis());
        
        WebSocketMessage message = new WebSocketMessage(MessageType.STATUS_CHANGE, payload);
        messagingTemplate.convertAndSend(destination, message);
    }
} 