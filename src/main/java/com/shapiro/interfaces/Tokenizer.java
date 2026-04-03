package com.shapiro.interfaces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

public interface Tokenizer {
    Stream<String> tokenize(String input);
    
    public default Stream<String> indexFile(Path file) {
        try {
            String content = Files.readString(file);
            LoggerFactory.getLogger(this.getClass()).info(content);
            return tokenize(content);
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Failed to read file: " + file, e);
        }
        return Stream.empty();
    };
}
