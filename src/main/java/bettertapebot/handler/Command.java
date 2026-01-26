package bettertapebot.handler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

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

    CODE(CommandLevel.LOGGED_IN, "/code", "zeigt den aktuellen Freischaltcode an"),
    USERS(CommandLevel.LOGGED_IN, "/users", "zeig alle registrierten Benutzer an"),
    ADD(CommandLevel.LOGGED_IN, "/add", "fügt ein neues Tape hinzu"),
    LAST(CommandLevel.LOGGED_IN, "/last", "liefert das letzte Tape"),
    ALL(CommandLevel.LOGGED_IN, "/all", "gibt alle Tapes aus"),
    STARRING(CommandLevel.LOGGED_IN, "/starring", "zeigt alle Tapes bei denen ein User mitspielt"),
    DIRECTING(CommandLevel.LOGGED_IN, "/directing", "zeigt alle Tapes die von einem User eingereicht wurden"),
    SUBSCRIPTION(CommandLevel.LOGGED_IN, "/subscription", "Empfang von Benachrichtigungen zu neuen Tapes aktiveren bzw. deaktivieren"),
    LOGOUT(CommandLevel.LOGGED_IN, "/logout", "loggt den aktuellen nutzer aus"),
    
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
            if(command.getCommand().equalsIgnoreCase(in)){
                return command;
            }
        }
        return null;
    }
    
    public enum CommandLevel {
        GENERAL, LOGGED_IN, ADMIN
    }
}
