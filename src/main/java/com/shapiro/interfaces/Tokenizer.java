package com.shapiro.interfaces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

public interface Tokenizer {
    Stream<String> tokenize(String input);
    
    public default Stream<String> tokenizeFile(Path file) {
        try {
            String content = Files.readString(file);
            Stream<String> tokens = tokenize(content);
            return (tokens == null) ? Stream.empty() : tokens;
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Failed to read file: " + file, e);
        }
        return Stream.empty();
    };
}
