package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    /**
     * 配置消息代理
     * 
     * @param registry 消息代理注册表
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单的基于内存的消息代理
        // 客户端订阅地址的前缀，例如 /topic/batch/123
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 客户端发送消息的目的地前缀，例如 /app/batch/123/subscribe
        registry.setApplicationDestinationPrefixes("/app");
        
        // 用户专有的目的地前缀，例如 /user/123/queue/messages
        registry.setUserDestinationPrefix("/user");
    }
    
    /**
     * 注册STOMP端点
     * 
     * @param registry STOMP端点注册表
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过这个端点连接到WebSocket服务器
        // 使用SockJS作为备选方案，以支持不支持WebSocket的浏览器
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 使用setAllowedOriginPatterns而不是setAllowedOrigins
                .withSockJS();
    }
} 