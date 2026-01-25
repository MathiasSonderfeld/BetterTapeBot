package bettertapebot.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = TapeEntity.TABLE_NAME)
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TapeEntity {

    public static final String TABLE_NAME = "tapes";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String title;
    
    /*
      we only need the username, as that's the foreign key, it can be accessed without cost
      So no need to load the rest eagerly if at all
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director", referencedColumnName = "username", nullable = false)
    UserEntity director;
    
    /*
      we only need the username, as that's the foreign key, it can be accessed without cost
      So no need to load the rest eagerly if at all
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "star", referencedColumnName = "username", nullable = false)
    UserEntity star;

    @Column(name = "date_added")
    Instant dateAdded;
}
