package components.services;

import components.entities.Room;
import components.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }
    public Room create(Room room) {
       return roomRepository.save(room);
    }
    public  Room findById(Long id) {
        return roomRepository.findById(id).orElse(null);
    }
}
