package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.config.PasscodeGenerator;
import bettertapebot.handler.Command;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserEntity;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.testutil.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, RegisterHandler.class, BotProperties.class, PasscodeGenerator.class})
class RegisterHandlerTest {
    
    @Autowired
    RegisterHandler registerHandler;
    
    @MockitoBean
    ResponseService responseService;
    
    @MockitoSpyBean
    PasscodeGenerator passcodeGenerator;
    
    @MockitoSpyBean
    UserRepository userRepository;
    
    @MockitoSpyBean
    UserStateRepository userStateRepository;
    
    @Autowired
    BotProperties botProperties;
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommandAndStates(){
        assertThat(registerHandler.forCommand()).isEqualTo(Command.REGISTER);
        assertThat(registerHandler.forStates()).containsExactlyInAnyOrder(
            UserState.REGISTER_AWAITING_ACTIVATION_CODE,
            UserState.REGISTER_AWAITING_DSGVO,
            UserState.REGISTER_AWAITING_USERNAME,
            UserState.REGISTER_AWAITING_PIN
        );
    }
    
    @Test
    public void alreadyLoggedInUserGetsInfo(){
        long chatId = 1234L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .isAdmin(false)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(userEntity)
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, chatId, "1234");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Du bist bereits registriert und eingeloggt");
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository, userRepository);
    }
    
    @Test
    public void newChatGetsGreetedAndAskedForActivationToken(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, chatId, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(3)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(3)
            .anyMatch(c -> c.contains("Hi!"))
            .anyMatch(c -> c.contains("Bock ein paar Tapes zu tracken?"))
            .anyMatch(c -> c.contains("Du brauchst einen Aktivierungscode von einem Mitglied."));
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_ACTIVATION_CODE);
    }
    
    @Test
    public void newChatWithCodeGetsGreetedAndAskedForUnparsableCode(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, chatId, "asdf");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(3)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(3)
            .anyMatch(c -> c.contains("Hi!"))
            .anyMatch(c -> c.contains("Bock ein paar Tapes zu tracken?"))
            .anyMatch(c -> c.contains("Der Aktivierungscode ist ungültig."));
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_ACTIVATION_CODE);
    }
    
    @Test
    public void chatWithActivationCodeGetsAskedAgainIfCodeIsWrong(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_ACTIVATION_CODE)
            .build());
        var code = passcodeGenerator.generatePasscode() + 10;
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, chatId, String.format("%04d", code));
        Mockito.verify(passcodeGenerator, Mockito.times(1)).validatePasscode(code);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Der Aktivierungscode ist ungültig.");
        Mockito.verifyNoInteractions(userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_ACTIVATION_CODE);
    }
    
    @Test
    public void chatWithActivationCodeGetsAskedForGdpr(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_ACTIVATION_CODE)
            .build());
        var code = passcodeGenerator.generatePasscode();
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, chatId, String.format("%04d", code));
        Mockito.verify(passcodeGenerator, Mockito.times(1)).validatePasscode(code);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReplyKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(ReplyKeyboardMarkup.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), markupCaptor.capture(), textCaptor.capture());
        assertKeyboardWithAcceptOrDenyOrCommandExists(markupCaptor.getAllValues());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nice")
            .contains("Leider sind wir in Deutschland und Datenschutz ist wichtig... 😒")
            .contains("Bitte bestätige, dass ich Deine Daten für diesen Dienst speichern und verarbeiten darf.")
            .contains(Command.DSGVO.getCommand());
        Mockito.verifyNoInteractions(userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_DSGVO);
    }
    
    
    private void assertKeyboardWithAcceptOrDenyOrCommandExists(List<ReplyKeyboardMarkup> markups) {
        assertThat(markups).isNotNull()
            .hasSize(1);
        var markup = markups.getFirst();
        assertThat(markup.getKeyboard()).hasSize(1);
        var keyboardRow = markup.getKeyboard().getFirst();
        assertThat(keyboardRow.size()).isEqualTo(3);
        assertThat(keyboardRow.getFirst().getText()).isEqualTo(botProperties.getAcceptGdprText());
        assertThat(keyboardRow.get(1).getText()).isEqualTo(botProperties.getDenyGdprText());
        assertThat(keyboardRow.get(2).getText()).isEqualTo(Command.DSGVO.getCommand());
    }
}