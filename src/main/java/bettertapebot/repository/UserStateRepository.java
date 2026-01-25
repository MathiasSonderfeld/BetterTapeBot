package bettertapebot.repository;

import bettertapebot.repository.entity.UserEntity;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserStateRepository extends JpaRepository<UserStateEntity, Long> {
    
    List<UserStateEntity> findUserStateEntitiesByUserStateIn(Collection<UserState> userStates);
    
    long deleteUserStateEntitiesByOwner(UserEntity owner);
}
