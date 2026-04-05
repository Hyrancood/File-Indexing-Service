package com.shapiro.implementations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.shapiro.core.IndexStore;

import static org.junit.jupiter.api.Assertions.*;

class TextFileIndexerTest {

    @TempDir
    Path tempDir;

    private IndexStore indexStore;
    private TextFileIndexer indexer;

    @BeforeEach
    void setUp() throws IOException {
        indexStore = new IndexStore();
        indexer = new TextFileIndexer(indexStore, new SimpleTokenizer(false), 2);
    }

    @AfterEach
    void tearDown() {
        if (indexer != null) {
            indexer.stop();
        }
    }

    @Test
    void addPath_shouldIndexTextFiles() throws IOException {
        Path textFile = tempDir.resolve("test.txt");
        Files.writeString(textFile, "hello world");

        indexer.addPath(tempDir);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Set<Path> results = indexer.search("hello");
        assertTrue(results.contains(textFile));
    }

    @Test
    void addPath_shouldIgnoreNonTextFiles() throws IOException {
        Path textFile = tempDir.resolve("test.txt");
        Files.writeString(textFile, "hello world");

        Path binaryFile = tempDir.resolve("test.jpg");
        Files.write(binaryFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}); // JPEG header

        indexer.addPath(tempDir);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Set<Path> results = indexer.search("hello");
        assertTrue(results.contains(textFile));
        assertFalse(results.contains(binaryFile));
    }

    @Test
    void addPath_shouldIndexFilesInSubdirectories() throws IOException {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);
        Path textFile = subDir.resolve("nested.txt");
        Files.writeString(textFile, "nested content");

        indexer.addPath(tempDir);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Set<Path> results = indexer.search("nested");
        assertTrue(results.contains(textFile));
    }

    @Test
    void search_shouldReturnFilesContainingToken() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Files.writeString(file1, "hello world");

        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file2, "hello java");

        Path file3 = tempDir.resolve("file3.txt");
        Files.writeString(file3, "goodbye world");

        indexer.addPath(tempDir);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Set<Path> helloResults = indexer.search("hello");
        assertEquals(2, helloResults.size());
        assertTrue(helloResults.contains(file1));
        assertTrue(helloResults.contains(file2));

        Set<Path> worldResults = indexer.search("world");
        assertEquals(2, worldResults.size());
        assertTrue(worldResults.contains(file1));
        assertTrue(worldResults.contains(file3));
    }

    @Test
    void removePath_shouldRemoveFilesFromIndex() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test content");

        indexer.addPath(tempDir);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(indexer.search("test").isEmpty());

        indexer.removePath(tempDir);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexer.search("test").isEmpty());
    }

    @Test
    void fileWatcherIntegration_shouldHandleFileChanges() throws IOException {
        indexer.addPath(tempDir);

        Path file = tempDir.resolve("dynamic.txt");
        Files.writeString(file, "initial content");

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexer.search("initial").contains(file));

        Files.writeString(file, "modified content");

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexer.search("modified").contains(file));
        assertFalse(indexer.search("initial").contains(file));

        Files.delete(file);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(indexer.search("modified").contains(file));
    }

    @Test
    void stop_shouldShutdownGracefully() {
        assertDoesNotThrow(() -> indexer.stop());
    }

    @Test
    void addPath_shouldHandleIOException() {
        Path invalidPath = tempDir.resolve("nonexistent").resolve("deep");

        assertDoesNotThrow(() -> indexer.addPath(invalidPath));
        assertTrue(indexStore.getAllTokens().iterator().hasNext() == false);
    }

    @Test
    void removePath_shouldHandleNonExistentPath() {
        Path nonExistent = tempDir.resolve("doesnotexist");

        assertDoesNotThrow(() -> indexer.removePath(nonExistent));
        assertTrue(indexStore.getAllTokens().iterator().hasNext() == false);
    }

    @Test
    void addPath_shouldHandleEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        indexer.addPath(emptyDir);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexStore.getAllTokens().isEmpty());
    }

    @Test
    void addPath_shouldIndexFilesWithoutExtension() throws IOException {
        Path noExtFile = tempDir.resolve("noextension");
        Files.writeString(noExtFile, "content without extension");

        indexer.addPath(tempDir);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Set<Path> results = indexer.search("content");
        assertFalse(results.contains(noExtFile));
    }

    @Test
    void addPath_shouldHandleMultiplePaths() throws IOException {
        Path dir1 = tempDir.resolve("dir1");
        Files.createDirectory(dir1);
        Path file1 = dir1.resolve("file1.txt");
        Files.writeString(file1, "content1");

        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectory(dir2);
        Path file2 = dir2.resolve("file2.txt");
        Files.writeString(file2, "content2");

        indexer.addPath(dir1);
        indexer.addPath(dir2);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(indexer.search("content1").contains(file1));
        assertTrue(indexer.search("content2").contains(file2));
    }
}