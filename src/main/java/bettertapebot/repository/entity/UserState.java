package bettertapebot.repository.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum UserState {
    //unhandled - user needs to send command to trigger event
    NEW_CHAT(StateLevel.NONE),
    LOGGED_OUT(StateLevel.NONE),
    LOGGED_IN(StateLevel.LOGGEDIN),
    ADMIN(StateLevel.ADMIN),

    //Login and Register
    LOGIN_VALIDATE_USERNAME(StateLevel.NONE),
    LOGIN_VALIDATE_PIN(StateLevel.NONE),
    REGISTER_AWAITING_DSGVO(StateLevel.NONE),
    REGISTER_AWAITING_ACTIVATION_CODE(StateLevel.NONE),
    REGISTER_AWAITING_USERNAME(StateLevel.NONE),
    REGISTER_AWAITING_PIN(StateLevel.NONE),

    //Logged In
    ADD_TAPE_GET_TITLE(StateLevel.LOGGEDIN),
    ADD_TAPE_GET_STAR(StateLevel.LOGGEDIN),
    STARRING_GET_USERNAME(StateLevel.LOGGEDIN),
    DIRECTING_GET_USERNAME(StateLevel.LOGGEDIN),
    SUBSCRIPTION_AWAITING_VALUE(StateLevel.LOGGEDIN),
    
    // Admin
    DELETE_USER_GET_USERNAME(StateLevel.ADMIN),
    DELETE_TAPE_GET_TAPE_ID(StateLevel.ADMIN),
    RESET_USER_GET_USERNAME(StateLevel.ADMIN),
    NEW_ADMIN_USER_GET_USERNAME(StateLevel.ADMIN),
    REMOVE_ADMIN_USER_GET_USERNAME(StateLevel.ADMIN),
    BROADCAST_AWAIT_MESSAGE(StateLevel.ADMIN);

    StateLevel stateLevel;

    public boolean isLoggedIn(){
        return isAdmin() || this.stateLevel == StateLevel.LOGGEDIN;
    }

    public boolean isAdmin() {
        return this.stateLevel == StateLevel.ADMIN;
    }
    
    public enum StateLevel {
        NONE, LOGGEDIN, ADMIN
    }
}
