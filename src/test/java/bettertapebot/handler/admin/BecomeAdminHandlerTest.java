package bettertapebot.handler.admin;

import bettertapebot.bot.ResponseService;
import bettertapebot.handler.Command;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserEntity;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.testutil.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    
    @MockitoSpyBean
    UserRepository userRepository;

    @MockitoBean
    ResponseService responseService;
    
    @BeforeEach
    void reset(){
        Mockito.reset(
            userRepository,
            userStateRepository,
            responseService
        );
    }
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void registersForCorrectCommand(){
        assertThat(becomeAdminHandler.forCommand()).isEqualTo(Command.ADMIN);
    }

    @Test
    public void unknownSessionGetsDenied(){
        long chatId = 1234L;
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
        long chatId = 2345L;

        var user = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());

        var state = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.ADMIN)
            .owner(user)
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
        long chatId = 3456L;

        var user = userRepository.save(UserEntity.builder()
            .username("user")
            .pin("1234")
            .isAdmin(false)
            .build());

        var state = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(user)
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
        long chatId = 4567L;

        var user = userRepository.save(UserEntity.builder()
            .username("admin")
            .pin("1234")
            .isAdmin(true)
            .build());

        var state = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(user)
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