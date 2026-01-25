package bettertapebot.repository;

import bettertapebot.repository.entity.TapeEntity;
import bettertapebot.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TapeRepository extends JpaRepository<TapeEntity, UUID> {
    
    List<TapeEntity> findAllByDirector(UserEntity userEntity);
    List<TapeEntity> findAllByStar(UserEntity userEntity);
    Optional<TapeEntity> deleteTapeEntityById(UUID id);
}
