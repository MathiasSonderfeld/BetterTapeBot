package bettertapebot.repository.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum UserState {
    //unhandled - user needs to send command to trigger event
    NEW_CHAT(false),
    LOGGED_OUT(false),
    LOGGED_IN(true),

    //Login and Register
    LOGIN_VALIDATE_USERNAME(false),
    LOGIN_VALIDATE_PIN(false),
    REGISTER_AWAITING_DSGVO(false),
    REGISTER_AWAITING_ACTIVATION_CODE(false),
    REGISTER_AWAITING_USERNAME(false),
    REGISTER_AWAITING_PIN(false),

    //Logged In
    ADD_TAPE_GET_TITLE(true),
    ADD_TAPE_GET_STAR(true),
    STARRING_GET_USERNAME(true),
    DIRECTING_GET_USERNAME(true),
    SUBSCRIPTION_AWAITING_VALUE(true),
    
    // Admin
    DELETE_USER_GET_USERNAME(true),
    DELETE_TAPE_GET_TAPE_ID(true),
    RESET_USER_GET_USERNAME(true),
    NEW_ADMIN_USER_GET_USERNAME(true),
    REMOVE_ADMIN_USER_GET_USERNAME(true),
    BROADCAST_AWAIT_MESSAGE(true);

    @Getter
    boolean loggedIn;
    
    public static final Set<UserState> LOGGED_IN_STATES = Arrays.stream(UserState.values())
        .filter(UserState::isLoggedIn)
        .collect(Collectors.toSet());
}
