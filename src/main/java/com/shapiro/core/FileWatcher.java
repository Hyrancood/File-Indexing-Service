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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
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
    private volatile boolean running = true;

    public FileWatcher(IndexStore indexStore, Tokenizer tokenizer, ExecutorService executor) throws IOException {
        this.indexStore = indexStore;
        this.tokenizer = tokenizer;
        this.executor = executor;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    public void registerRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
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
        });
    }

    @Override
    public void run() {
        try {
            while (running) {
                WatchKey key = watchService.take();
                Path dir = keys.get(key);
                if (dir == null) {
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
                                tokenizer.indexFile(child).forEach(token -> indexStore.add(token, child));
                            });
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        executor.submit(() -> indexStore.update(child, tokenizer.indexFile(child)));
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        indexStore.removeFile(child);
                    }
                }
                if (!key.reset()) {
                    keys.remove(key);
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred while watching file system", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
        try {
            watchService.close();
        } catch (IOException e) {
            // logger
        }
    }

}
