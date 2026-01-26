package bettertapebot.handler.general;

import bettertapebot.bot.ResponseService;
import bettertapebot.config.PasscodeGenerator;
import bettertapebot.handler.Command;
import bettertapebot.handler.CommandHandler;
import bettertapebot.handler.StateHandler;
import bettertapebot.properties.BotProperties;
import bettertapebot.repository.UserRepository;
import bettertapebot.repository.UserStateRepository;
import bettertapebot.repository.entity.UserEntity;
import bettertapebot.repository.entity.UserState;
import bettertapebot.repository.entity.UserStateEntity;
import bettertapebot.util.MessageCleaner;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Set;
import java.util.regex.Pattern;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegisterHandler implements CommandHandler, StateHandler {

    private static final Set<UserState> REGISTER_START = Set.of(UserState.NEW_CHAT, UserState.LOGGED_OUT);
    private static final String MEMBERS_CAN_GET_CODE = String.format("Mit %s können alle eingeloggten Mitglieder einen gültigen Freischaltcode anzufordern! 🤯", Command.CODE.getCommand());
    private static final Pattern USERNAME_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]{2,255}$");
    private static final Pattern PIN_REGEX = Pattern.compile("^[0-9]{4}$");
    
    BotProperties botProperties;
    ResponseService responseService;
    PasscodeGenerator passcodeGenerator;
    UserRepository userRepository;
    UserStateRepository userStateRepository;
    
    @NonFinal
    ReplyKeyboardMarkup dsgvoMarkup;
    
    @PostConstruct
    void postConstruct(){
        dsgvoMarkup = ReplyKeyboardMarkup.builder()
            .keyboardRow(new KeyboardRow(botProperties.getAcceptGdprText(), botProperties.getDenyGdprText(), Command.DSGVO.getCommand()))
            .build();
    }
    
    @Override
    public @NonNull Command forCommand() {
        return Command.REGISTER;
    }
    
    @Override
    public @NonNull Set<UserState> forStates() {
        return Set.of(
            UserState.REGISTER_AWAITING_ACTIVATION_CODE,
            UserState.REGISTER_AWAITING_DSGVO,
            UserState.REGISTER_AWAITING_USERNAME,
            UserState.REGISTER_AWAITING_PIN
        );
    }

    @Override
    @Transactional
    public void handleMessage(@NonNull UserStateEntity userStateEntity, long chatId, String message) {
        //check if logged in
        if(userStateEntity.getUserState().isLoggedIn()){
            responseService.send(userStateEntity.getChatId(), "Du bist bereits registriert und eingeloggt");
            return;
        }
        
        //greet new chats
        if(userStateEntity.getUserState() == UserState.NEW_CHAT){
            responseService.send(userStateEntity.getChatId(), "Hi! :)");
            responseService.send(userStateEntity.getChatId(), "Bock ein paar Tapes zu tracken? 😏");
        }
        
        //verify activation code, it was either passed directly as parameter with /register or sent on request
        if(REGISTER_START.contains(userStateEntity.getUserState())){
            if(!StringUtils.hasText(message)){
                userStateEntity.setUserState(UserState.REGISTER_AWAITING_ACTIVATION_CODE);
                responseService.send(userStateEntity.getChatId(), "Du brauchst einen Aktivierungscode von einem Mitglied. " + MEMBERS_CAN_GET_CODE);
            }
            else {
                var givenCode = MessageCleaner.getFirstWord(message);
                validateActivationCode(userStateEntity, givenCode);
            }
            return;
        }
        if(userStateEntity.getUserState() == UserState.REGISTER_AWAITING_ACTIVATION_CODE){
            var givenCode = MessageCleaner.getFirstWord(message);
            validateActivationCode(userStateEntity, givenCode);
            return;
        }
        
        if(userStateEntity.getUserState() == UserState.REGISTER_AWAITING_DSGVO){
            var response = MessageCleaner.getFirstWord(message);
            validateGdprResponse(userStateEntity, response);
            return;
        }
        
        if(userStateEntity.getUserState() == UserState.REGISTER_AWAITING_USERNAME){
            var username = MessageCleaner.getFirstWord(message);
            validateUsername(userStateEntity, username);
            return;
        }
        
        var pin = MessageCleaner.getFirstWord(message);
        validatePin(userStateEntity, pin);
    }
    
    private void validateActivationCode(UserStateEntity userStateEntity, String givenCode) {
        int code;
        try{
            code = Integer.parseInt(givenCode);
        }
        catch (NumberFormatException e){
            userStateEntity.setUserState(UserState.REGISTER_AWAITING_ACTIVATION_CODE);
            responseService.send(userStateEntity.getChatId(), "Der Aktivierungscode ist ungültig. " + MEMBERS_CAN_GET_CODE);
            return;
        }
        
        if(!passcodeGenerator.validatePasscode(code)){
            userStateEntity.setUserState(UserState.REGISTER_AWAITING_ACTIVATION_CODE);
            responseService.send(userStateEntity.getChatId(), "Der Aktivierungscode ist ungültig. " + MEMBERS_CAN_GET_CODE);
        }
        else {
            userStateEntity.setUserState(UserState.REGISTER_AWAITING_DSGVO);
            responseService.send(userStateEntity.getChatId(), "Nice! 😎"); //TODO send is async, merge dis
            responseService.send(userStateEntity.getChatId(),"Leider sind wir in Deutschland und Datenschutz ist wichtig... 😒");
            responseService.send(userStateEntity.getChatId(), dsgvoMarkup,String.format("Bitte bestätige, dass ich Deine Daten für diesen Dienst speichern und verarbeiten darf. Gib %s ein, um genaueres zu erfahren.", Command.DSGVO.getCommand()));
        }
    }
    
    private void validateGdprResponse(UserStateEntity userStateEntity, String response) {
        if(botProperties.getDenyGdprText().equalsIgnoreCase(response)){
            userStateRepository.deleteById(userStateEntity.getChatId());
            responseService.send(userStateEntity.getChatId(), "Tut mir Leid, aber ohne Einverständnis kann ich dich nicht reinlassen. Ich habe alle Informationen über diesen Chat gelöscht. Ciao!");
            return;
        }
        if(!botProperties.getAcceptGdprText().equalsIgnoreCase(response)){
            responseService.send(userStateEntity.getChatId(), dsgvoMarkup, "Die Antwort konnte ich nicht auswerten. Bitte bestätige, dass ich deine Daten für diesen Dienst speichern und verarbeiten darf.");
            return;
        }
        userStateEntity.setUserState(UserState.REGISTER_AWAITING_USERNAME);
        responseService.send(userStateEntity.getChatId(), "Toll, das hat geklappt! 🥳");
        responseService.send(userStateEntity.getChatId(), "Wie soll dein Benutzername lauten? Er wird bei den Tapes angezeigt und du brauchst den für den Login 😊");
    }
    
    private void validateUsername(UserStateEntity userStateEntity, String username) {
        if(!USERNAME_REGEX.matcher(username).matches()){
            responseService.send(userStateEntity.getChatId(), "Der Username hat ein ungültiges Format. Erlaubte Zeichen sind A-Z, a-z, +, _, ., -");
            return;
        }
        
        boolean userExists = userRepository.existsById(username);
        if(userExists){
            responseService.send(userStateEntity.getChatId(), "Den Benutzernamen kennen wir schon! 👀");
            responseService.send(userStateEntity.getChatId(), String.format("benutze einen anderen oder verwende %s um dich anzumelden", Command.RESET.getCommand()));
            return;
        }
        
        var userEntity = userRepository.save(UserEntity.builder()
            .username(username)
            .pin("xxxx")
            .build());
        userStateEntity.setOwner(userEntity);
        userStateEntity.setUserState(UserState.REGISTER_AWAITING_PIN);
        
        responseService.send(userStateEntity.getChatId(), "Juhu 🎉");
        responseService.send(userStateEntity.getChatId(), "Dein Benutzername lautet: " + username);
        responseService.send(userStateEntity.getChatId(), "Denke dir jetzt eine 4-stellige PIN aus. Du brauchst sie später, um dich erneut einzuloggen.");
    }
    
    private void validatePin(UserStateEntity userStateEntity, String pin) {
        if(!PIN_REGEX.matcher(pin).matches()){
            responseService.send(userStateEntity.getChatId(), "Die PIN hat ein ungültiges Format. Ich brauche 4 Ziffern.");
            return;
        }
        userStateEntity.getOwner().setPin(pin);
        userStateEntity.setUserState(UserState.LOGGED_IN);
        responseService.send(userStateEntity.getChatId(), "Yeah! 🥳");
        responseService.send(userStateEntity.getChatId(), "Du bist jetzt eingeloggt!");
        responseService.send(userStateEntity.getChatId(), String.format("Benutze %s um neue Tapes hinzuzufügen oder %s für weitere Funktionen ♥️", Command.ADD.getCommand(), Command.HELP.getCommand()));
    }
}
