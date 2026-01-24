package eu.sonderfeld.mathias.bettertapebot.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = TapeEntity.TABLE_NAME)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TapeEntity {

    public static final String TABLE_NAME = "tapes";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String title;

    @ManyToOne
    @JoinColumn(name = "author", referencedColumnName = "username", nullable = false)
    UserEntity author;

    @ManyToOne
    @JoinColumn(name = "star", referencedColumnName = "username", nullable = false)
    UserEntity star;

    @Column(name = "date_added")
    Instant dateAdded;
}
