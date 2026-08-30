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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к этой доске");
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
    public boolean isOwner(Userat user, Room room) {
        if (room != null) {
            return (user.getRole() == Role.TEACHER) || (room.getOwnerId() != null && room.getOwnerId().equals(user.getId()));
        }
        return false;
    }
    public void requireOwner(Userat user, Room room) {
        if(!isOwner(user, room)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ заблокирован, только владелец или учитель может удалить доску");
        }
    }
}