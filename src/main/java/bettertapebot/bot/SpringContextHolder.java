package bettertapebot.bot;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextHolder implements ApplicationContextAware {
    
    @Getter
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(@NonNull ApplicationContext ctx) {
        context = ctx;
    }
}

