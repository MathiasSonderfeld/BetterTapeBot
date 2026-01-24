package eu.sonderfeld.mathias.bettertapebot.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = UserStateEntity.TABLE_NAME)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStateEntity {

    public static final String TABLE_NAME = "user_states";

    @Id
    @Column(name = "chat_id")
    Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_state", nullable = false)
    UserState userState;

    @ManyToOne
    @JoinColumn(name = "username", referencedColumnName = "username")
    @OnDelete(action = OnDeleteAction.CASCADE)
    UserEntity user;
}
