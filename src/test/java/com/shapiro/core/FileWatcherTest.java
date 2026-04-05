package com.shapiro.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.shapiro.implementations.SimpleTokenizer;
import com.shapiro.implementations.TextFileIndexer;

import static org.junit.jupiter.api.Assertions.*;

class FileWatcherTest {

    @TempDir
    Path tempDir;

    private IndexStore indexStore;
    private SimpleTokenizer tokenizer;
    private ExecutorService executor;
    private FileWatcher watcher;
    private Thread watcherThread;

    @BeforeEach
    void setUp() throws IOException {
        indexStore = new IndexStore();
        tokenizer = new SimpleTokenizer(false);
        executor = Executors.newFixedThreadPool(2);
        watcher = new FileWatcher(indexStore, tokenizer, executor, TextFileIndexer::isPlainTextFile);
        watcherThread = new Thread(watcher);
        watcherThread.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (watcher != null) {
            watcher.stop();
        }
        if (watcherThread != null && watcherThread.isAlive()) {
            watcherThread.join(1000);
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void registerRecursively_shouldRegisterDirectoryAndSubdirectories() throws IOException {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);

        watcher.registerRecursively(tempDir);

        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "hello world");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexStore.search("hello").contains(testFile));
    }

    @Test
    void fileCreation_shouldTriggerIndexing() throws IOException {
        watcher.registerRecursively(tempDir);

        Path newFile = tempDir.resolve("newfile.txt");
        Files.writeString(newFile, "test content");

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexStore.search("test").contains(newFile));
    }

    @Test
    void fileModification_shouldUpdateIndex() throws IOException {
        Path file = tempDir.resolve("modify.txt");
        Files.writeString(file, "old content");
        watcher.registerRecursively(tempDir);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Files.writeString(file, "new content", StandardOpenOption.TRUNCATE_EXISTING);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexStore.search("new").contains(file));
        assertFalse(indexStore.search("old").contains(file));
    }

    @Test
    void fileDeletion_shouldRemoveFromIndex() throws IOException {
        Path file = tempDir.resolve("delete.txt");
        Files.writeString(file, "content");
        watcher.registerRecursively(tempDir);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Files.delete(file);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(indexStore.search("content").contains(file));
    }

    @Test
    void directoryCreation_shouldRegisterNewDirectory() throws IOException {
        watcher.registerRecursively(tempDir);

        Path newDir = tempDir.resolve("newdir");
        Files.createDirectory(newDir);
        Path fileInNewDir = newDir.resolve("file.txt");
        Files.writeString(fileInNewDir, "nested content");

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexStore.search("nested").contains(fileInNewDir));
    }

    @Test
    void stop_shouldShutdownWatcherGracefully() throws IOException, InterruptedException {
        watcher.registerRecursively(tempDir);
        watcher.stop();
        watcherThread.join(1000);

        assertFalse(watcherThread.isAlive(), "Watcher thread should stop after watcher.stop()");
    }

    @Test
    void registerRecursively_shouldHandleIOException() {
        Path invalidPath = tempDir.resolve("nonexistent").resolve("deep");

        assertThrows(IOException.class, () -> watcher.registerRecursively(invalidPath));
    }

    @Test
    void registerRecursively_shouldAllowReregisteringSameDirectory() throws IOException {
        watcher.registerRecursively(tempDir);
        assertDoesNotThrow(() -> watcher.registerRecursively(tempDir));
    }

    @Test
    void fileCreation_shouldIgnoreNonFilteredFiles() throws IOException {
        watcher.registerRecursively(tempDir);

        Path binaryFile = tempDir.resolve("binary.jpg");
        Files.write(binaryFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(indexStore.search("binary").contains(binaryFile));
    }

    @Test
    void run_shouldStopAfterServiceClose() throws IOException, InterruptedException {
        watcher.registerRecursively(tempDir);
        watcher.stop();

        watcherThread.join(1000);
        assertFalse(watcherThread.isAlive(), "Watcher thread should stop after closing WatchService");
    }
}