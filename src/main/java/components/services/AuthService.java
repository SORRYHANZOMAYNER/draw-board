package components.services;

import components.dto.AuthResponse;
import components.dto.LoginRequest;
import components.dto.RegisterRequest;
import components.entities.Role;
import components.entities.Userat;
import components.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    public AuthResponse  register(RegisterRequest request){
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        Userat userat = new Userat();
        userat.setUsername(request.getUsername().trim());
        userat.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userat.setRole(Role.STUDENT);
        userat.setCreateAt(new Date());
        return toAuthResponse(userRepository.save(userat),true);
    }
    public AuthResponse getMe(Userat user) {
        return toAuthResponse(user, false);
    }
    public AuthResponse  login(LoginRequest request){
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
        Userat userat = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), userat.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return toAuthResponse(userat, true);
    }
    public Userat findById(Long id){
        return userRepository.findById(id).orElse(null);
    }
    private AuthResponse toAuthResponse(Userat user, boolean withToken) {
        String token = withToken ? jwtService.generateToken(user) : null;
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}
