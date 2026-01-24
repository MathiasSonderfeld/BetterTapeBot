package eu.sonderfeld.mathias.bettertapebot.repository;

import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStateRepository extends JpaRepository<UserStateEntity, Long> {
}
