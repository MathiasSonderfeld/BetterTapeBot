package eu.sonderfeld.mathias.bettertapebot.repository;

import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
}
