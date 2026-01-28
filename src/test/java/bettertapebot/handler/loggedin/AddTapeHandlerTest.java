package bettertapebot.handler.loggedin;

import bettertapebot.bot.ResponseService;
import bettertapebot.cache.TapeCache;
import bettertapebot.cache.TapeCacheEntry;
import bettertapebot.handler.Command;
import bettertapebot.repository.TapeRepository;
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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, AddTapeHandler.class, TapeCache.class})
class AddTapeHandlerTest {
    
    @Autowired
    AddTapeHandler addTapeHandler;
    
    @MockitoSpyBean
    UserStateRepository userStateRepository;
    
    @MockitoSpyBean
    UserRepository userRepository;
    
    @MockitoSpyBean
    TapeRepository tapeRepository;
    
    @MockitoBean
    ResponseService responseService;
    
    @MockitoBean
    TapeCache tapeCache;
    
    @AfterEach
    void cleanUp(){
        userStateRepository.deleteAll();
        tapeRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    public void registersForCorrectCommandAndStates(){
        assertThat(addTapeHandler.forCommand()).isEqualTo(Command.ADD);
        assertThat(addTapeHandler.forStates()).containsExactlyInAnyOrder(UserState.ADD_TAPE_GET_STAR, UserState.ADD_TAPE_GET_TITLE);
    }
    
    @Test
    public void notLoggedInUserGetsDenied(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.NEW_CHAT)
            .build());
        
        Mockito.reset(tapeCache, userStateRepository, userRepository, tapeRepository, responseService);
        addTapeHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Nur eingeloggte User können Tapes eintragen");
        Mockito.verifyNoInteractions(tapeCache, userStateRepository, userRepository, tapeRepository);
    }
    
    @Test
    public void addWithoutTitleGetsAskedForTitle(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .build());
        
        Mockito.reset(tapeCache, userStateRepository, userRepository, tapeRepository, responseService);
        addTapeHandler.handleMessage(userStateEntity, null);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Welchen Titel soll das Werk tragen?");
        Mockito.verifyNoInteractions(tapeCache, userStateRepository, userRepository, tapeRepository);
        
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.ADD_TAPE_GET_TITLE);
    }
    
    @Test
    public void addWithTitleGetsPutInCacheAndAskedForStar(){
        long chatId = 1234L;
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.LOGGED_IN)
            .build());
        var title = "testtitle1";
        
        Mockito.reset(tapeCache, userStateRepository, userRepository, tapeRepository, responseService);
        addTapeHandler.handleMessage(userStateEntity, title);
        Mockito.verify(tapeCache, Mockito.times(1)).put(chatId, title);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Wer ist der Star dieses Meisterwerks?");
        Mockito.verifyNoInteractions(userStateRepository, userRepository, tapeRepository);
        
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.ADD_TAPE_GET_STAR);
    }
    
    @Test
    public void addWithUnknownStarGetsAskedAgain(){
        long chatId = 1234L;
        var requestorEntity = userRepository.save(UserEntity.builder()
            .username("star")
            .pin("9876")
            .build());
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.ADD_TAPE_GET_STAR)
            .owner(requestorEntity)
            .build());
        var unknownUser = "unknownUser";
        
        Mockito.reset(tapeCache, userStateRepository, userRepository, tapeRepository, responseService);
        addTapeHandler.handleMessage(userStateEntity, unknownUser);
        Mockito.verify(userRepository, Mockito.times(1)).findById(unknownUser);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).send(ArgumentMatchers.eq(chatId), textCaptor.capture());
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains("Den Benutzer gibt es nicht. Probiers nochmal");
        Mockito.verifyNoInteractions(tapeCache, userStateRepository, tapeRepository);
        
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.ADD_TAPE_GET_STAR);
    }
    
    @Test
    public void addWithKnownStarGetsAdded(){
        long chatId = 1234L;
        long starChatId = 2234L;
        long otherActiveChatId = 3456L;
        long otherNotActiveChatId = 4567L;
        long mutedChatId = 5678L;
        
        var requestorEntity = userRepository.save(UserEntity.builder()
            .username("requestor")
            .pin("1234")
            .build());
        var starEntity = userRepository.save(UserEntity.builder()
            .username("star")
            .pin("9876")
            .build());
        var otherEntity = userRepository.save(UserEntity.builder()
            .username("someone")
            .pin("8765")
            .build());
        var mutedEntity = userRepository.save(UserEntity.builder()
            .username("afk")
            .pin("7654")
            .wantsAbonnement(false)
            .build());
        
        var userStateEntity = userStateRepository.save(UserStateEntity.builder()
            .chatId(chatId)
            .userState(UserState.ADD_TAPE_GET_STAR)
            .owner(requestorEntity)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .chatId(starChatId)
            .userState(UserState.LOGGED_IN)
            .owner(starEntity)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .chatId(otherActiveChatId)
            .userState(UserState.LOGGED_IN)
            .owner(otherEntity)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .chatId(otherNotActiveChatId)
            .userState(UserState.LOGGED_OUT)
            .owner(otherEntity)
            .build());
        userStateRepository.save(UserStateEntity.builder()
            .chatId(mutedChatId)
            .userState(UserState.LOGGED_IN)
            .owner(mutedEntity)
            .build());
        
        ZonedDateTime time = ZonedDateTime.of(2026,1,1,12,0,0,0, ZoneOffset.UTC);
        String expectedTime = "01.01.26 13:00";
        
        String tapeTitle = "testtitle";
        TapeCacheEntry cacheEntry = new TapeCacheEntry(tapeTitle, time.toInstant());
        Mockito.when(tapeCache.get(ArgumentMatchers.anyLong())).thenReturn(cacheEntry);
        
        Mockito.reset(userStateRepository, userRepository, tapeRepository, responseService);
        addTapeHandler.handleMessage(userStateEntity, starEntity.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).findById(starEntity.getUsername());
        Mockito.verify(tapeCache, Mockito.times(1)).get(chatId);
        Mockito.verify(tapeCache, Mockito.times(1)).remove(chatId);
        Mockito.verify(tapeRepository, Mockito.times(1)).save(ArgumentMatchers.any());
        Mockito.verify(userStateRepository, Mockito.times(1)).findAllByUserStateIsInAndOwner_WantsAbonnement(ArgumentMatchers.anyCollection(), ArgumentMatchers.eq(true));
        
        //noinspection unchecked
        ArgumentCaptor<List<Long>> chatIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(responseService, Mockito.times(1)).broadcast(chatIdsCaptor.capture(), textCaptor.capture());
        var capturedIdLists = chatIdsCaptor.getAllValues();
        assertThat(capturedIdLists).hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
            .containsExactlyInAnyOrder(chatId, starChatId, otherActiveChatId)
            .doesNotContain(otherNotActiveChatId, mutedChatId);
        
        var texts = textCaptor.getAllValues();
        assertThat(texts).isNotNull()
            .hasSize(1)
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(starEntity.getUsername())
            .contains(tapeTitle)
            .contains(expectedTime);
        
        assertThat(userStateEntity.getUserState()).isEqualTo(UserState.LOGGED_IN);
    }
}