package bettertapebot.repository.entity;

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

    /*
      most times we only need the username, as that's the foreign key, it can be accessed without cost
      So we can load the rest lazy in the few cases we need more
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner", referencedColumnName = "username")
    @OnDelete(action = OnDeleteAction.CASCADE)
    UserEntity owner;
}
