package components.security;
import components.entities.Userat;
import components.services.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;
@Component
public class WebSocketJwtHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ATTRIBUTE = "wsUser";
    private final JwtService jwtService;
    public WebSocketJwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Userat user = jwtService.validateTokenAndLoadUser(token);
            attributes.put(USER_ATTRIBUTE, user);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}