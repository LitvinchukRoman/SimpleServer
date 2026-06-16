package practice2.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@DisplayName("TextUtils")
class TextUtilsTest {

    @ParameterizedTest(name = "\"{0}\" is palindrome")
    @Tag("fast")
    @ValueSource(strings = {"level", "radar", "12321", "Able was I ere I saw Elba"})
    void recognizesPalindromes(String candidate) {
        assertTrue(TextUtils.isPalindrome(candidate));
    }

    @TestFactory
    @Tag("slow")
    Stream<DynamicTest> reverseIsReversible() {
        List<String> samples = List.of("hello", "JUnit6", "Litvinchuk", "12345", "");
        return samples.stream()
                .map(s -> dynamicTest(
                        "double reverse \"" + s + "\"",
                        () -> assertEquals(s, TextUtils.reverse(TextUtils.reverse(s)))
                ));
    }
}
