package com.shapiro.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.shapiro.interfaces.Tokenizer;

public class SimpleTokenizer implements Tokenizer {
    private final boolean caseSensitive;

    public SimpleTokenizer(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public Stream<String> tokenize(String input) {
        if (input == null || input.isEmpty()) {
            return Stream.empty();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                currentToken.append(c);        
            } else if (currentToken.length() > 0) {
                tokens.add(currentToken.toString());
                currentToken.setLength(0);
            }
        }
        if (currentToken.length() > 0) {
            String token = currentToken.toString();
            if (!caseSensitive) {
                token = token.toLowerCase();
            }
            tokens.add(token);
        }
        return tokens.stream();
    }
}
