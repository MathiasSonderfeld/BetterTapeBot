package bettertapebot.config;

import bettertapebot.properties.BotProperties;
import bettertapebot.repository.UserRepository;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Random;

@CustomLog
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InitialAdminCodeGenerator implements ApplicationRunner {
    
    BotProperties botProperties;
    UserRepository userRepository;
    
    @Getter
    @NonFinal
    int initialAdminKey = -1;
    
    @Override
    public void run(@NonNull ApplicationArguments args) {
        long users = userRepository.countUserEntitiesByUsernameNot(botProperties.getDefaultUserForTapes());
        if(users == 0){
            Random random = new Random();
            initialAdminKey = random.nextInt(1000, 1000000);
            log.info("Identified fresh setup, providing initial admin key: {}", initialAdminKey);
        }
    }
    
    public boolean validatesInitialAdminKey(int input) {
        if(initialAdminKey < 0){
            return false;
        }
        
        if(input == initialAdminKey){
            //reset admin key as it was used
            initialAdminKey = -1;
            return true;
        }
        return false;
    }
}
