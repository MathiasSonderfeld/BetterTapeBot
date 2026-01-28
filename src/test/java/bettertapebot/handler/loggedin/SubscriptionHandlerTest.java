package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, SubscriptionHandler.class, BotProperties.class})
class SubscriptionHandlerTest {
    
    @Autowired
    SubscriptionHandler subscriptionHandler;
    
    @MockitoBean
    ResponseService responseService;
    
    @Autowired
    BotProperties botProperties;
    
    @Autowired
    UserStateRepository userStateRepository;
    
    @Autowired
    UserRepository userRepository;
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommandAndStates(){
        assertThat(subscriptionHandler.forCommand()).isEqualTo(Command.SUBSCRIPTION);
        assertThat(subscriptionHandler.forStates()).containsExactlyInAnyOrder(UserState.SUBSCRIPTION_AWAITING_VALUE);
    }
    
    @Test
    public void notLoggedInUserGetsDenied(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(responseService);
        subscriptionHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können ihren Updates-Status ändern");
    }
    
    @Test
    public void subscriptionUpdateWithoutValueGetsAskedForWantedStatus(){
        long chatId = 2345L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(userEntity)
            .build());
        
        Mockito.reset(responseService);
        subscriptionHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReplyKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(ReplyKeyboardMarkup.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), markupCaptor.capture(), textCaptor.capture());
        assertOneTemporaryKeyboardWithYesAndNoExists(markupCaptor.getAllValues());
        
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Möchtest du weiterhin Updates bekommen?");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.SUBSCRIPTION_AWAITING_VALUE);
    }
    
    @Test
    public void subscriptionUpdateWithUninterpretableValueGetsAskedAgain() {
        long chatId = 4567L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("username")
            .pin("1234")
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .owner(userEntity)
            .build());
        String uninterpretableValue = "I dont know!";
        
        Mockito.reset(responseService);
        subscriptionHandler.handleMessage(userStateEntity, uninterpretableValue);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReplyKeyboardMarkup> markupCaptor = ArgumentCaptor.forClass(ReplyKeyboardMarkup.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), markupCaptor.capture(), textCaptor.capture());
        assertOneTemporaryKeyboardWithYesAndNoExists(markupCaptor.getAllValues());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Das konnte ich nicht interpretieren. Möchtest du weiterhin Updates bekommen?");
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.SUBSCRIPTION_AWAITING_VALUE);
    }
    
    @Test
    public void subscriptionWithTrueGetsActivated(){
        long chatId = 6789L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("requestor")
            .pin("1234")
            .isAdmin(false)
            .wantsAbonnement(false)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.SUBSCRIPTION_AWAITING_VALUE)
            .owner(userEntity)
            .build());
        
        Mockito.reset(responseService);
        subscriptionHandler.handleMessage(userStateEntity, botProperties.getSubscription().getCountsAsYes().getFirst());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Updates sind aktiviert");
        
        assertThat(userEntity.getWantsAbonnement()).isTrue();
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
    
    @Test
    public void subscriptionWithFalseGetsDeactivated(){
        long chatId = 7890L;
        var userEntity = userRepository.save(UserEntity.builder()
            .username("requestor")
            .pin("1234")
            .isAdmin(false)
            .wantsAbonnement(true)
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.SUBSCRIPTION_AWAITING_VALUE)
            .owner(userEntity)
            .build());
        
        Mockito.reset(responseService);
        subscriptionHandler.handleMessage(userStateEntity, botProperties.getSubscription().getCountsAsNo().getFirst());
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Updates sind deaktiviert");
        
        assertThat(userEntity.getWantsAbonnement()).isFalse();
        
        assertThat(userStateEntity)
            .extracting(UserStateEntity::getUserState)
            .isEqualTo(UserState.LOGGED_IN);
    }
    
    private void assertOneTemporaryKeyboardWithYesAndNoExists(List<ReplyKeyboardMarkup> markups) {
        assertThat(markups).isNotNull()
            .hasSize(1);
        var markup = markups.getFirst();
        assertThat(markup.getOneTimeKeyboard()).isTrue();
        assertThat(markup.getKeyboard()).hasSize(1);
        var keyboardRow = markup.getKeyboard().getFirst();
        assertThat(keyboardRow.size()).isEqualTo(2);
        assertThat(keyboardRow.getFirst().getText()).isEqualTo(botProperties.getSubscription().getCountsAsYes().getFirst());
        assertThat(keyboardRow.get(1).getText()).isEqualTo(botProperties.getSubscription().getCountsAsNo().getFirst());
    }
}