package eu.sonderfeld.mathias.bettertapebot.handler.admin;

import eu.sonderfeld.mathias.bettertapebot.bot.ResponseService;
import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import eu.sonderfeld.mathias.bettertapebot.repository.UserRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.UserStateRepository;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserEntity;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserState;
import eu.sonderfeld.mathias.bettertapebot.repository.entity.UserStateEntity;
import eu.sonderfeld.mathias.bettertapebot.util.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, BecomeAdminHandler.class})
class BecomeAdminHandlerTest {

    @Autowired
    BecomeAdminHandler becomeAdminHandler;

    @MockitoSpyBean
    UserStateRepository userStateRepository;

    @MockitoBean
    ResponseService responseService;
    @Autowired
    private UserRepository userRepository;


    @Test
    public void registersForCorrectCommand(){
        assertThat(becomeAdminHandler.forCommand()).isEqualTo(Command.ADMIN);
    }

    @Test
    public void unknownSessionGetsDenied(){
        Long chatId = 1234L;
        becomeAdminHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können in den Admin-Modus wechseln");
    }

    @Test
    public void alreadyAdminGetsInformed(){
        Long chatId = 2345L;

        var user = userRepository.save(UserEntity.builder()
            .username("admin")
            .isAdmin(true)
            .build());

        var state = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.ADMIN)
            .user(user)
            .build());

        becomeAdminHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Du bist bereits im Admin-Modus");
        assertThat(state.getUserState()).isEqualTo(UserState.ADMIN);
    }

    @Test
    public void normalUserGetsDenied(){
        Long chatId = 3456L;

        var user = userRepository.save(UserEntity.builder()
            .username("user")
            .isAdmin(false)
            .build());

        var state = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .user(user)
            .build());

        becomeAdminHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Du bist kein Admin");
        assertThat(state.getUserState()).isEqualTo(UserState.LOGGED_IN);
    }

    @Test
    public void adminCanUpgrade(){
        Long chatId = 4567L;

        var user = userRepository.save(UserEntity.builder()
            .username("admin")
            .isAdmin(true)
            .build());

        var state = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .user(user)
            .build());

        becomeAdminHandler.handleCommand(chatId, "testmessage");
        Mockito.verify(userStateRepository, Mockito.times(1)).findById(chatId);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Du bist in den Admin-Bereich gewechselt");
        assertThat(state.getUserState()).isEqualTo(UserState.ADMIN);
    }

}