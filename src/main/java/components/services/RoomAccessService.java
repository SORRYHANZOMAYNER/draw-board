package components.services;
import components.entities.Role;
import components.entities.Room;
import components.entities.Userat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class RoomAccessService {
    public void checkAccess(Userat user, Room room) {
        if (!canAccess(user, room)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this room");
        }
    }
    public boolean canAccess(Userat user, Room room) {
        if (room == null) {
            return false;
        }
        if (room.getOwnerId() != null && room.getOwnerId().equals(user.getId())) {
            return true;
        }
        return user.getRole() == Role.TEACHER;
    }
}