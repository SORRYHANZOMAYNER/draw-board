package components.entities;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "drawing_events")
public class DrawingEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String payload;

    @Column(nullable = false)
    private Date createdAt = new Date();

    public DrawingEventEntity() {
    }

    public DrawingEventEntity(Long roomId, String payload) {
        this.roomId = roomId;
        this.payload = payload;
        this.createdAt = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
