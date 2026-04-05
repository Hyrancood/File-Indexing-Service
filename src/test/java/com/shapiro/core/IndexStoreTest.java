package com.shapiro.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndexStoreTest {

    @Test
    void addAndSearch_shouldReturnPathForToken() {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        store.add("hello", path);

        Set<Path> result = store.search("hello");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(path));
    }

    @Test
    void addDuplicateTokenAndPath_shouldNotDuplicateSearchResults() {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        store.add("hello", path);
        store.add("hello", path);

        Set<Path> result = store.search("hello");

        assertEquals(1, result.size(), "Duplicate token additions should not create duplicate results");
    }

    @Test
    void removeToken_shouldRemovePathFromSearch() {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        store.add("hello", path);
        store.remove("hello", path);

        assertTrue(store.search("hello").isEmpty());
    }

    @Test
    void update_shouldReplaceOldTokensForPath() {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        store.add("one", path);
        store.add("two", path);

        store.update(path, java.util.stream.Stream.of("three", "four"));

        assertTrue(store.search("one").isEmpty());
        assertTrue(store.search("two").isEmpty());
        assertEquals(Set.of(path), store.search("three"));
        assertEquals(Set.of(path), store.search("four"));
    }

    @Test
    void removeFile_shouldClearAllTokensForPath() {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        store.add("one", path);
        store.add("two", path);
        store.removeFile(path);

        assertTrue(store.search("one").isEmpty());
        assertTrue(store.search("two").isEmpty());
        assertFalse(store.getAllIndexedFiles().iterator().hasNext());
    }

    @Test
    void addAndSearch_shouldSupportRussianTokens() {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        store.add("привет", path);
        store.add("мир", path);

        assertEquals(Set.of(path), store.search("привет"));
        assertEquals(Set.of(path), store.search("мир"));
    }

    @Test
    void addAndSearch_shouldSupportMixedRussianEnglishTokens() {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        store.add("приветHello", path);
        store.add("helloПривет", path);

        assertEquals(Set.of(path), store.search("приветHello"));
        assertEquals(Set.of(path), store.search("helloПривет"));
    }

    @Test
    void getAllIndexedFiles_andGetAllTokens_shouldReturnCurrentContent() {
        IndexStore store = new IndexStore();
        Path path1 = Paths.get("/tmp/file1.txt");
        Path path2 = Paths.get("/tmp/file2.txt");

        store.add("hello", path1);
        store.add("world", path2);
        store.add("hello", path2);

        assertTrue(store.getAllIndexedFiles().iterator().hasNext());
        assertTrue(store.getAllTokens().iterator().hasNext());
        assertEquals(Set.of(path1, path2), Set.copyOf(store.getAllIndexedFiles()));
        assertEquals(Set.of("hello", "world"), Set.copyOf(store.getAllTokens()));
    }

    @Test
    void concurrentAddAndRemove_shouldMaintainConsistency() throws ExecutionException, InterruptedException {
        IndexStore store = new IndexStore();
        Path path = Paths.get("/tmp/file.txt");

        CompletableFuture<Void> addTask = CompletableFuture.runAsync(() -> {
            IntStream.range(0, 1000).forEach(i -> store.add("token-" + i, path));
        });

        CompletableFuture<Void> removeTask = CompletableFuture.runAsync(() -> {
            IntStream.range(0, 1000).forEach(i -> store.remove("token-" + i, path));
        });

        CompletableFuture.allOf(addTask, removeTask).get();

        assertTrue(store.getAllIndexedFiles().iterator().hasNext() || store.search("token-0").isEmpty());
    }
}
