package com.abhishekojha.kurakanimonolith.common.config;

import com.abhishekojha.kurakanimonolith.common.security.WebSocketAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://kurakaani.me"
    };

    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")   // connection establishment
                .setAllowedOrigins(ALLOWED_ORIGINS)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        config.enableSimpleBroker("/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}
