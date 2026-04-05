package com.shapiro.core;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

public class IndexStore {
    private final ConcurrentHashMap<String, Set<Path>> index = new ConcurrentHashMap<>();
    private final Map<Path, Set<String>> words = new ConcurrentHashMap<>();

    public void add(String token, Path path) {
        index.computeIfAbsent(token, k -> new CopyOnWriteArraySet<>()).add(path);
        words.computeIfAbsent(path, k -> new CopyOnWriteArraySet<>()).add(token);
    }

    public void remove(String token, Path path) {
        index.getOrDefault(token, new CopyOnWriteArraySet<>()).remove(path);
        words.getOrDefault(path, new CopyOnWriteArraySet<>()).remove(token);
    }

    public void update(Path path, Stream<String> newTokens) {
        Set<String> oldTokens = words.getOrDefault(path, new CopyOnWriteArraySet<>());
        oldTokens.forEach(token -> remove(token, path));
        newTokens.forEach(token -> add(token, path));
    }

    public Set<Path> search(String token) {
        return index.getOrDefault(token, new CopyOnWriteArraySet<>());
    }

    public void removeFile(Path path) {
        index.values().forEach(set -> set.remove(path));
        words.remove(path);
    }

    public Collection<Path> getAllIndexedFiles() {
        return words.keySet();
    }

    public Collection<String> getAllTokens() {
        return index.keySet();
    }
}
