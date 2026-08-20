package com.rongan.security_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类，使用 STOMP 协议
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 注册 STOMP 端点，前端通过这个端点连接 WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")          // WebSocket 连接地址
                .setAllowedOriginPatterns("*") // 允许所有来源 仅限开发环境
                .withSockJS();                // 启用 SockJS 支持
    }

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单代理，客户端订阅 /topic 开头的地址即可接收消息
        registry.enableSimpleBroker("/topic");
        // 设置应用前缀 客户端发送消息到服务端时使用 本系统不需要
        registry.setApplicationDestinationPrefixes("/app");
    }
}
