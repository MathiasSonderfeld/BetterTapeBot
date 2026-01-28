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
        registerHandler.handleMessage(userStateEntity, "1234");
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
        registerHandler.handleMessage(userStateEntity, null);
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
        registerHandler.handleMessage(userStateEntity, "asdf");
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
        registerHandler.handleMessage(userStateEntity, String.format("%04d", code));
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
        registerHandler.handleMessage(userStateEntity, String.format("%04d", code));
        Mockito.verify(passcodeGenerator, Mockito.times(1)).validatePasscode(code);
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReplyKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(ReplyKeyboardMarkup.class);
        Mockito.verify(responseService, Mockito.times(2)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), markupCaptor.capture(), textCaptor.capture());
        assertKeyboardWithAcceptOrDenyOrCommandExists(markupCaptor.getAllValues());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(3);
        assertThat(texts.getFirst()).contains("Nice");
        assertThat(texts.get(1)).contains("Leider sind wir in Deutschland und Datenschutz ist wichtig... 😒");
        assertThat(texts.get(2)).contains("Bitte bestätige, dass ich deine Daten für diesen Dienst speichern und verarbeiten darf.")
            .contains(Command.DSGVO.getCommand())
            .contains("ein, um Genaueres zu erfahren.");
        Mockito.verifyNoInteractions(userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_DSGVO);
    }
    
    @Test
    public void chatWithGdprNoGetsReset(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_DSGVO)
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, botProperties.getDenyGdprText());
        Mockito.verify(userStateRepository, Mockito.times(1)).deleteById(chatId);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(1);
        assertThat(texts.getFirst()).isEqualTo("Tut mir Leid, aber ohne Einverständnis kann ich dich nicht reinlassen. Ich habe alle Informationen über diesen Chat gelöscht. Ciao!");
        Mockito.verifyNoInteractions(passcodeGenerator, userRepository);
        var updatedState = userStateRepository.findById(chatId);
        assertThat(updatedState).isNotNull().isEmpty();
    }
    
    @Test
    public void chatWithInvalidGdprGetsAskedAgain(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_DSGVO)
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, "I dont know!");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReplyKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(ReplyKeyboardMarkup.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), markupCaptor.capture(), textCaptor.capture());
        assertKeyboardWithAcceptOrDenyOrCommandExists(markupCaptor.getAllValues());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(1);
        assertThat(texts.getFirst()).isEqualTo("Die Antwort konnte ich nicht auswerten. Bitte bestätige, dass ich deine Daten für diesen Dienst speichern und verarbeiten darf.");
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_DSGVO);
    }
    
    @Test
    public void chatWithGdprYesGetsAskedForUsername(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_DSGVO)
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, botProperties.getAcceptGdprText());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(2)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(2);
        assertThat(texts.getFirst()).isEqualTo("Toll, das hat geklappt! 🥳");
        assertThat(texts.get(1)).isEqualTo("Wie soll dein Benutzername lauten? Er wird bei den Tapes angezeigt und du brauchst den für den Login 😊");
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_USERNAME);
    }
    
    @Test
    public void chatWithInvalidUsernameGetsAskedAgain(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_USERNAME)
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, "🥳🥳🥳");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(1);
        assertThat(texts.getFirst()).isEqualTo("Der Username hat ein ungültiges Format. Erlaubte Zeichen sind A-Z, a-z, +, _, ., -");
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository, userRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_USERNAME);
    }
    
    @Test
    public void chatWithExistingUsernameGetsAskedAgain(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_USERNAME)
            .build());
        
        String preexistingUsername = "preexisting";
        userRepository.save(UserEntity.builder()
            .username(preexistingUsername)
            .pin("9876")
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, preexistingUsername);
        Mockito.verify(userRepository, Mockito.times(1)).existsById(preexistingUsername);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(2)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(2);
        assertThat(texts.getFirst()).isEqualTo("Den Benutzernamen kennen wir schon! 👀");
        assertThat(texts.get(1)).contains("Benutze einen anderen oder verwende")
            .contains(Command.LOGIN.getCommand());
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_USERNAME);
    }
    
    @Test
    public void chatWithValidUsernameGetsAskedForPin(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_USERNAME)
            .build());
        String username = "username";
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, username);
        Mockito.verify(userRepository, Mockito.times(1)).existsById(username);
        Mockito.verify(userRepository, Mockito.times(1)).save(ArgumentMatchers.assertArg(entity -> {
           assertThat(entity.getUsername()).isEqualTo(username);
            assertThat(entity.getPin()).isEqualTo("xxxx");
            assertThat(entity.getIsAdmin()).isFalse();
            assertThat(entity.getWantsAbonnement()).isTrue();
        }));
        
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(3)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(3);
        assertThat(texts.getFirst()).isEqualTo("Juhu 🎉");
        assertThat(texts.get(1)).contains("Dein Benutzername lautet:").contains(username);
        assertThat(texts.get(2)).isEqualTo("Denke dir jetzt eine 4-stellige PIN aus. Du brauchst sie später, um dich erneut einzuloggen.");
        Mockito.verifyNoInteractions(passcodeGenerator, userStateRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_PIN);
        assertThat(userStateEntity.getOwner()).isNotNull()
            .extracting(UserEntity::getUsername).isEqualTo(username);
    }
    
    @Test
    public void chatWithInvalidPinGetsAskedAgain(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_PIN)
            .build());
        userRepository.save(UserEntity.builder()
            .username("username")
            .pin("xxxx")
            .build());
        
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, "abcd");
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(1);
        assertThat(texts.getFirst()).isEqualTo("Die PIN hat ein ungültiges Format. Ich brauche 4 Ziffern.");
        Mockito.verifyNoInteractions(passcodeGenerator, userRepository, userStateRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.REGISTER_AWAITING_PIN);
    }
    
    @Test
    public void chatWithValidPinGetsLoggedIn(){
        long chatId = 1234L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("xxxx")
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.REGISTER_AWAITING_PIN)
            .owner(userEntity)
            .build());
        
        String pin = "1234";
        Mockito.reset(passcodeGenerator, userStateRepository, userRepository, responseService);
        registerHandler.handleMessage(userStateEntity, pin);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(3)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull().hasSize(3);
        assertThat(texts.getFirst()).isEqualTo("Yeah! 🥳");
        assertThat(texts.get(1)).contains("Du bist jetzt eingeloggt!");
        assertThat(texts.get(2)).contains(Command.ADD.getCommand()).contains("um neue Tapes hinzuzufügen").contains(Command.HELP.getCommand());
        Mockito.verifyNoInteractions(passcodeGenerator, userRepository, userStateRepository);
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.LOGGED_IN);
        assertThat(userStateEntity.getOwner()).isNotNull()
            .extracting(UserEntity::getPin).isEqualTo(pin);
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