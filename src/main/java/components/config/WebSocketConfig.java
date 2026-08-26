package components.config;

import components.security.StompAuthChannelInterceptor;
import components.security.WebSocketJwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketJwtHandshakeInterceptor handshakeInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    public WebSocketConfig(
            WebSocketJwtHandshakeInterceptor handshakeInterceptor,
            StompAuthChannelInterceptor stompAuthChannelInterceptor
    ) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Точка подключения WebSocket
        registry.addEndpoint("/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(512 * 1024);
        registration.setSendBufferSizeLimit(512 * 1024);
    }
}