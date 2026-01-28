package bettertapebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BetterTapeBotApplication {
    static void main(String[] args) {
        SpringApplication.run(BetterTapeBotApplication.class, args);
    }
}
