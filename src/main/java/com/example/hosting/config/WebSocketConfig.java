package com.example.hosting.config;

import com.example.hosting.handler.StreamHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(streamHandler(), "/stream")
                .setAllowedOriginPatterns("*")  // Allow the frontend origin
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Bean
    public StreamHandler streamHandler() {
        return new StreamHandler();
    }

    // Increase the WebSocket buffer size
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(512 * 10240);  // Set to 5120KB
        container.setMaxTextMessageBufferSize(512 * 1024);    // Set to 512KB
        container.setMaxSessionIdleTimeout(60000L);           // Optional: Set idle timeout to 60 seconds
        return container;
    }
}
