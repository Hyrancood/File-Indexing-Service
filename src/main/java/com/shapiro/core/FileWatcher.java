package com.shapiro.core;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shapiro.interfaces.Tokenizer;

public class FileWatcher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);
    
    private final WatchService watchService;
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final IndexStore indexStore;
    private final Tokenizer tokenizer;
    private final Predicate<Path> fileFilter;
    private volatile boolean running = true;

    public FileWatcher(IndexStore indexStore, Tokenizer tokenizer, ExecutorService executor, Predicate<Path> fileFilter) throws IOException {
        this.indexStore = indexStore;
        this.tokenizer = tokenizer;
        this.executor = executor;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.fileFilter = fileFilter;
    }

    public void registerRecursively(Path root) throws IOException {
        Files.walkFileTree(root.normalize(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE, 
                    StandardWatchEventKinds.ENTRY_DELETE, 
                    StandardWatchEventKinds.ENTRY_MODIFY
                );
                keys.put(key, dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isDirectory() && fileFilter.test(file)) {
                    executor.submit(() -> {
                        tokenizer.tokenizeFile(file).forEach(token -> indexStore.add(token, file));
                    });
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void unregisterRecursively(Path root) throws IOException {
        if (root == null) {
            return;
        }
        if (!Files.exists(root)) {
            keys.entrySet().removeIf(entry -> entry.getValue().startsWith(root));
            indexStore.getAllIndexedFiles().forEach(p -> {
                if (p.startsWith(root)) {
                    indexStore.removeFile(p);
                }
            });
            return;
        }
        HashSet<Path> pathToDelete = new HashSet<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                pathToDelete.add(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                pathToDelete.add(file);
                indexStore.removeFile(file);
                return FileVisitResult.CONTINUE;
            }
        });
        keys.entrySet().removeIf(entry -> pathToDelete.contains(entry.getValue()));
    }

    @Override
    public void run() {
        try {
            while (running) {
                WatchKey key = watchService.take();
                Path dir = keys.get(key);
                if (dir == null) {
                    if (!key.reset()) keys.remove(key);
                    continue;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path name = (Path) event.context();
                    Path child = dir.resolve(name);
                    
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (Files.isDirectory(child)) {
                            registerRecursively(child);
                        } else {
                            executor.submit(() -> {
                                tokenizer.tokenizeFile(child).forEach(token -> indexStore.add(token, child));
                            });
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        if (Files.isRegularFile(child) && fileFilter.test(child)) {
                            executor.submit(() -> indexStore.update(child, tokenizer.tokenizeFile(child)));
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        executor.submit(() -> {
                            indexStore.getAllIndexedFiles().forEach(p -> {
                                if (p.startsWith(child)) {
                                    indexStore.removeFile(p);
                                }
                            });
                        });
                    }
                }
                if (!key.reset()) {
                    keys.remove(key);
                    try {
                        Files.walk(dir)
                            .filter(Files::isRegularFile)
                            .forEach(indexStore::removeFile);
                    } catch (IOException e) {
                        // Директория уже недоступна, игнорируем
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred while watching file system", e);
        } catch (InterruptedException e) {
            logger.error("File watcher thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
        try {
            watchService.close();
        } catch (IOException e) {
            logger.error("Failed to close WatchService", e);
        }
    }

}
