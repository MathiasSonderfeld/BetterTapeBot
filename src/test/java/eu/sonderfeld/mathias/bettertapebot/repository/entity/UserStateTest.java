package eu.sonderfeld.mathias.bettertapebot.repository.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class UserStateTest {
    
    @Test
    void noneStateIsNotLoggedIn(){
        var noneStates = Arrays.stream(UserState.values())
            .filter(c -> c.getStateLevel() == UserState.StateLevel.NONE)
            .collect(Collectors.toSet());
        
        assertThat(noneStates).noneMatch(UserState::isLoggedIn);
    }
    
    @Test
    void noneStateIsNotAdmin(){
        var noneStates = Arrays.stream(UserState.values())
            .filter(c -> c.getStateLevel() == UserState.StateLevel.NONE)
            .collect(Collectors.toSet());
        
        assertThat(noneStates).noneMatch(UserState::isAdmin);
    }
    
    @Test
    void loggedInStateIsLoggedIn(){
        var noneStates = Arrays.stream(UserState.values())
            .filter(c -> c.getStateLevel() == UserState.StateLevel.LOGGEDIN)
            .collect(Collectors.toSet());
        
        assertThat(noneStates).allMatch(UserState::isLoggedIn);
    }
    
    @Test
    void loggedInStateIsNotAdmin(){
        var noneStates = Arrays.stream(UserState.values())
            .filter(c -> c.getStateLevel() == UserState.StateLevel.LOGGEDIN)
            .collect(Collectors.toSet());
        
        assertThat(noneStates).noneMatch(UserState::isAdmin);
    }
    
    @Test
    void adminStateIsLoggedIn(){
        var noneStates = Arrays.stream(UserState.values())
            .filter(c -> c.getStateLevel() == UserState.StateLevel.ADMIN)
            .collect(Collectors.toSet());
        
        assertThat(noneStates).allMatch(UserState::isLoggedIn);
    }
    
    @Test
    void adminStateIsAdmin(){
        var noneStates = Arrays.stream(UserState.values())
            .filter(c -> c.getStateLevel() == UserState.StateLevel.ADMIN)
            .collect(Collectors.toSet());
        
        assertThat(noneStates).allMatch(UserState::isAdmin);
    }
}