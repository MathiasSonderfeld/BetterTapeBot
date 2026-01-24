package eu.sonderfeld.mathias.bettertapebot.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@Table(name = UserEntity.TABLE_NAME)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntity {

    public static final String TABLE_NAME = "users";

    @Id
    @Column(name = "username")
    String username;

    @Column(nullable = false, length = 4)
    String pin;

    @Builder.Default
    @Column(name = "is_admin")
    Boolean isAdmin = false;

    @Builder.Default
    @Column(name = "wants_abonnement")
    Boolean wantsAbonnement = true;
}
