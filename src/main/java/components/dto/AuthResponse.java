package components.dto;

import components.entities.Role;
import components.entities.User;

public class AuthResponse {

    private Long userId;
    private String username;
    private Role role;

    public AuthResponse() {
    }

    public AuthResponse(Long userId, String username, Role role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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