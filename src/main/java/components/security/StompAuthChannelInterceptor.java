package components.security;
import components.entities.Userat;
import components.entities.Room;
import components.repositories.RoomRepository;
import components.services.RoomAccessService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private static final Pattern ROOM_DESTINATION = Pattern.compile("^/app/room/(\\d+)/draw$");
    private final RoomRepository roomRepository;
    private final RoomAccessService roomAccessService;
    public StompAuthChannelInterceptor(RoomRepository roomRepository, RoomAccessService roomAccessService) {
        this.roomRepository = roomRepository;
        this.roomAccessService = roomAccessService;
    }
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Userat user = (Userat) accessor.getSessionAttributes().get(WebSocketJwtHandshakeInterceptor.USER_ATTRIBUTE);
            if (user == null) {
                throw new IllegalArgumentException("Unauthorized WebSocket connection");
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);
            return message;
        }
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Userat user = getCurrentUser(accessor);
            String destination = accessor.getDestination();
            if (destination != null) {
                Matcher matcher = ROOM_DESTINATION.matcher(destination);
                if (matcher.matches()) {
                    Long roomId = Long.parseLong(matcher.group(1));
                    Room room = roomRepository.findById(roomId).orElse(null);
                    roomAccessService.checkAccess(user, room);
                }
            }
        }
        return message;
    }
    private Userat getCurrentUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof Userat user) {
            return user;
        }
        Userat user = (Userat) accessor.getSessionAttributes().get(WebSocketJwtHandshakeInterceptor.USER_ATTRIBUTE);
        if (user == null) {
            throw new IllegalArgumentException("Unauthorized WebSocket message");
        }
        return user;
    }
}