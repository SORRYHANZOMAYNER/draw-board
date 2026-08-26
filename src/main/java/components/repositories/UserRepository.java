package components.repositories;

import components.entities.Role;
import components.entities.Userat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Userat, Long> {
    Optional<Userat> findByUsername(String username);
    boolean existsByUsername(String username);
    List<Userat> findByUsernameContainingIgnoreCaseAndRole(String username, Role role);
}
