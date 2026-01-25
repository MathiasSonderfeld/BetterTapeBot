package bettertapebot.handler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum Command {
    REGISTER(CommandLevel.GENERAL, "/register", "registriert einen neuen User"),
    LOGIN(CommandLevel.GENERAL, "/login", "logt den user ein"),
    DSGVO(CommandLevel.GENERAL, "/dsgvo", "zeigt die Datenschutzbestimmungen"),
    ME(CommandLevel.GENERAL, "/me", "gibt chatId, username und user state zurück"),
    HELP(CommandLevel.GENERAL, "/help", "zeigt diesen Dialog an"),
    RESET(CommandLevel.GENERAL, "/reset", "setzt den Chat zurück"),

    CODE(CommandLevel.LOGGEDIN, "/code", "zeigt den aktuellen Freischaltcode an"),
    USERS(CommandLevel.LOGGEDIN, "/users", "zeig alle registrierten Benutzer an"),
    ADD(CommandLevel.LOGGEDIN, "/add", "fügt ein neues Tape hinzu"),
    LAST(CommandLevel.LOGGEDIN, "/last", "liefert das letzte Tape"),
    ALL(CommandLevel.LOGGEDIN, "/all", "gibt alle Tapes aus"),
    STARRING(CommandLevel.LOGGEDIN, "/starring", "zeigt alle Tapes bei denen ein User mitspielt"),
    DIRECTING(CommandLevel.LOGGEDIN, "/directing", "zeigt alle Tapes die von einem User eingereicht wurden"),
    SUBSCRIPTION(CommandLevel.LOGGEDIN, "/subscription", "Empfang von Benachrichtigungen zu neuen Tapes aktiveren bzw. deaktivieren"),
    LOGOUT(CommandLevel.LOGGEDIN, "/logout", "loggt den aktuellen nutzer aus"),
    
    ADMIN(CommandLevel.ADMIN, "/admin", "in den Admin-Modus wechseln"),
    DELETE_USER(CommandLevel.ADMIN, "/deleteuser", "ausgewählten User löschen"),
    DELETE_TAPE(CommandLevel.ADMIN, "/deletetape", "ausgewähltes Tape löschen"),
    RESET_USER(CommandLevel.ADMIN, "/resetuser", "alle Status-Einträge eines ausgewählten Users zurücksetzen"),
    NEW_ADMIN(CommandLevel.ADMIN, "/newadmin", "neuen Admin hinzufügen"),
    REMOVE_ADMIN(CommandLevel.ADMIN, "/removeadmin", "Admin entfernen"),
    BROADCAST(CommandLevel.ADMIN, "/broadcast", "erstellt einen Broadcast zu allen eingeloggten Usern"),
    EXIT(CommandLevel.ADMIN, "/exit", "adminmodus verlassen");

    CommandLevel commandLevel;
    String command;
    String helpText;

    public String getFormattedHelpText() {
        return String.format("%s - %s", this.getCommand(), this.getHelpText());
    }
    
    public static Command fromCommandString(String in){
        if(in == null){
            return null;
        }
        
        for (Command command : Command.values()) {
            if(Objects.equals(in, command.getCommand())){
                return command;
            }
        }
        return null;
    }
    
    public enum CommandLevel {
        GENERAL, LOGGEDIN, ADMIN
    }
}
