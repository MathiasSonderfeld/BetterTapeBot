package eu.sonderfeld.mathias.bettertapebot.repository.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum UserState {
    FIRST_TIME_ON_SERVER(StateLevel.NONE),
    LOGGED_OUT(StateLevel.NONE),

    // Registered user branch
    VALIDATE_USERNAME(StateLevel.NONE),
    VALIDATE_PIN(StateLevel.NONE),

    // New user branch
    AWAITING_DSGVO(StateLevel.NONE),
    AWAITING_ACTIVATION_CODE(StateLevel.NONE),
    REGISTER_USERNAME(StateLevel.NONE),
    REGISTER_PIN(StateLevel.NONE),

    // Default
    LOGGED_IN(StateLevel.LOGGEDIN),
    GET_TITLE(StateLevel.LOGGEDIN),
    GET_SUBJECT(StateLevel.LOGGEDIN),
    REPLY_BY_TAPES(StateLevel.LOGGEDIN),
    REPLY_FOR_TAPES(StateLevel.LOGGEDIN),

    // Admin
    ADMIN(StateLevel.ADMIN),
    GET_INPUT(StateLevel.ADMIN),
    CONFIRM_INPUT(StateLevel.ADMIN),

    // Other
    ERROR_RETRIEVING_STATE(StateLevel.NONE);

    StateLevel stateLevel;



    public enum StateLevel {
        NONE, LOGGEDIN, ADMIN
    }

    public boolean isLoggedIn(){
        return isAdmin() || this.stateLevel == StateLevel.LOGGEDIN;
    }

    public boolean isAdmin() {
        return this.stateLevel == StateLevel.ADMIN;
    }
}
