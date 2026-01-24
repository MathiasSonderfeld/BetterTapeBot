package eu.sonderfeld.mathias.bettertapebot.util;

import eu.sonderfeld.mathias.bettertapebot.handler.Command;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.stream.Stream;

@SpringJUnitConfig
class MessageCleanerTest {
    
    @Nested
    class CleanupTest {
        private static Stream<Arguments> provideStringsForCleanup() {
            return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", ""),
                Arguments.of("  ", ""),
                Arguments.of("message", "message"),
                Arguments.of(" message ", "message"),
                Arguments.of(" complete message ", "complete message")
            );
        }
        
        @ParameterizedTest
        @MethodSource("provideStringsForCleanup")
        void testCleanup(String input, String expected){
            var result = Assertions.assertDoesNotThrow(() -> MessageCleaner.cleanup(input));
            Assertions.assertEquals(expected, result);
        }
    }
    
    @Nested
    class GetAllWordsTest {
        private static Stream<Arguments> provideStringsForGetAllWords() {
            return Stream.of(
                Arguments.of(null, new String[]{""}),
                Arguments.of("", new String[]{""}),
                Arguments.of("  ", new String[]{""}),
                Arguments.of("message", new String[]{"message"}),
                Arguments.of(" message ", new String[]{"message"}),
                Arguments.of("complete message", new String[]{"complete", "message"}),
                Arguments.of(" complete message ", new String[]{"complete", "message"}),
                Arguments.of("complete  message", new String[]{"complete", "message"}),
                Arguments.of(" complete   message ", new String[]{"complete", "message"}),
                Arguments.of("1 2 3 4 5 6 7 8 9", new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"})
            );
        }
        
        @ParameterizedTest
        @MethodSource("provideStringsForGetAllWords")
        void testGetAllWords(String input, String[] expected){
            String[] result = Assertions.assertDoesNotThrow(() -> MessageCleaner.getAllWords(input));
            Assertions.assertArrayEquals(expected, result);
        }
    }
    
    @Nested
    class GetFirstWordTest {
        private static Stream<Arguments> provideStringsForGetFirstWord() {
            return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", ""),
                Arguments.of("  ", ""),
                Arguments.of("message", "message"),
                Arguments.of(" message ", "message"),
                Arguments.of("complete message", "complete"),
                Arguments.of(" complete message ", "complete"),
                Arguments.of("complete  message", "complete"),
                Arguments.of(" complete   message ", "complete"),
                Arguments.of("1 2 3 4 5 6 7 8 9", "1")
            );
        }
        
        @ParameterizedTest
        @MethodSource("provideStringsForGetFirstWord")
        void testGetFirstWord(String input, String expected){
            String result = Assertions.assertDoesNotThrow(() -> MessageCleaner.getFirstWord(input));
            Assertions.assertEquals(expected, result);
        }
    }
    
    @Nested
    class RemoveCommandTest {
        private static Stream<Arguments> provideValidStringsForRemoveCommand() {
            return Stream.of(
                Arguments.of(Command.ADMIN.getCommand() + "message", Command.ADMIN, "message"),
                Arguments.of(Command.ADMIN.getCommand() + " message", Command.ADMIN, "message"),
                Arguments.of(Command.ADMIN.getCommand() + "message peter pan, cleanup at last  ", Command.ADMIN, "message peter pan, cleanup at last"),
                Arguments.of(Command.ADMIN.getCommand() + "  message peter pan, cleanup at last  ", Command.ADMIN, "message peter pan, cleanup at last")
            );
        }
        
        private static Stream<Arguments> provideInvalidStringsForRemoveCommand() {
            return Stream.of(
                Arguments.of(null, Command.ADMIN, NullPointerException.class),
                Arguments.of("", null, NullPointerException.class),
                Arguments.of("", Command.ADMIN, IndexOutOfBoundsException.class)
            );
        }
        
        @ParameterizedTest
        @MethodSource("provideValidStringsForRemoveCommand")
        void testRemoveCommandWithValidInput(String input, Command command, String expected){
            String result = Assertions.assertDoesNotThrow(() -> MessageCleaner.removeCommand(input, command));
            Assertions.assertEquals(expected, result);
        }
        
        @ParameterizedTest
        @MethodSource("provideInvalidStringsForRemoveCommand")
        void testRemoveCommandWithInvalidInput(String input, Command command, Class<Throwable> clazz){
            Assertions.assertThrows(clazz, () -> MessageCleaner.removeCommand(input, command));
        }
    }
}