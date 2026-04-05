package com.shapiro.implementations;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTokenizerTest {

    private final SimpleTokenizer caseSensetiveTokenizer = new SimpleTokenizer(true);
    private final SimpleTokenizer caseInsensitiveTokenizer = new SimpleTokenizer(false);

    @Test
    void tokenize_shouldReturnEmptyStreamForNullInput() {
        assertTrue(caseSensetiveTokenizer.tokenize(null).findAny().isEmpty());
        assertTrue(caseInsensitiveTokenizer.tokenize(null).findAny().isEmpty());
    }

    @Test
    void tokenize_shouldReturnEmptyStreamForEmptyInput() {
        assertTrue(caseSensetiveTokenizer.tokenize("").findAny().isEmpty());
        assertTrue(caseInsensitiveTokenizer.tokenize("").findAny().isEmpty());
    }

    @Test
    void tokenize_shouldSplitOnWhitespace() {
        String input = "hello World  test\nline";
        List<String> caseSensitiveTokens = caseSensetiveTokenizer.tokenize(input).collect(Collectors.toList());
        List<String> caseInsensitiveTokens = caseInsensitiveTokenizer.tokenize(input).collect(Collectors.toList());

        assertEquals(List.of("hello", "World", "test", "line"), caseSensitiveTokens);
        assertEquals(List.of("hello", "world", "test", "line"), caseInsensitiveTokens);
    }

    @Test
    void tokenize_shouldPreserveNonWhitespaceCharacters() {
        List<String> tokens = caseSensetiveTokenizer.tokenize("a,b.c! d?e").collect(Collectors.toList());

        assertEquals(List.of("a", "b", "c", "d", "e"), tokens);
    }

    @Test
    void tokenize_shouldHandleLeadingAndTrailingWhitespace() {
        String input = "   hello  World  ";
        List<String> caseSensitiveTokens = caseSensetiveTokenizer.tokenize(input).collect(Collectors.toList());
        List<String> caseInsensitiveTokens = caseInsensitiveTokenizer.tokenize(input).collect(Collectors.toList());

        assertEquals(List.of("hello", "World"), caseSensitiveTokens);
        assertEquals(List.of("hello", "world"), caseInsensitiveTokens);
    }

    @Test
    void tokenize_shouldHandleMultipleWhitespaceCharacters() {
        String input = "hello\tWorld\n\rnew";
        List<String> caseSensitiveTokens = caseSensetiveTokenizer.tokenize(input).collect(Collectors.toList());
        List<String> caseInsensitiveTokens = caseInsensitiveTokenizer.tokenize(input).collect(Collectors.toList());

        assertEquals(List.of("hello", "World", "new"), caseSensitiveTokens);
        assertEquals(List.of("hello", "world", "new"), caseInsensitiveTokens);
    }

    @Test
    void tokenize_shouldSupportRussianText() {
        String input = "Привет, мир!";
        List<String> caseSensitiveTokens = caseSensetiveTokenizer.tokenize(input).collect(Collectors.toList());
        List<String> caseInsensitiveTokens = caseInsensitiveTokenizer.tokenize(input).collect(Collectors.toList());
        
        assertEquals(List.of("Привет", "мир"), caseSensitiveTokens);
        assertEquals(List.of("привет", "мир"), caseInsensitiveTokens);
    }

    @Test
    void tokenize_shouldSupportMixedRussianEnglishTokens() {
        String input = "Привет world Тест2024";
        List<String> caseSensitiveTokens = caseSensetiveTokenizer.tokenize(input).collect(Collectors.toList());
        List<String> caseInsensitiveTokens = caseInsensitiveTokenizer.tokenize(input).collect(Collectors.toList());

        assertEquals(List.of("Привет", "world", "Тест2024"), caseSensitiveTokens);
        assertEquals(List.of("привет", "world", "тест2024"), caseInsensitiveTokens);
    }
}
