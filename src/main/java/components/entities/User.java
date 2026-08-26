package components.entities;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private Role role;
    private Date createAt;
    @Column(nullable = false)
    private String passwordHash;

    public void setId(Long id) {
        this.id = id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
