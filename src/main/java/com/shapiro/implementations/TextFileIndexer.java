package com.shapiro.implementations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.shapiro.core.FileWatcher;
import com.shapiro.core.IndexStore;
import com.shapiro.interfaces.Indexer;
import com.shapiro.interfaces.Tokenizer;

public class TextFileIndexer implements Indexer {
    private final IndexStore store;
    private final Tokenizer tokenizer;
    private final FileWatcher watcher;
    private final ExecutorService workerPool;
    private final Thread watcherThread;

    public TextFileIndexer(IndexStore store, Tokenizer tokenizer, int threadPoolSize) throws IOException {
        this.store = store;
        this.tokenizer = tokenizer;
        this.workerPool = Executors.newFixedThreadPool(threadPoolSize);
        this.watcher = new FileWatcher(store, tokenizer, workerPool);
        this.watcherThread = new Thread(watcher);
        watcherThread.start();
    }

    @Override
    public void addPath(Path path) {
        try {
            watcher.registerRecursively(path);
            Files.walk(path)
            .filter(Files::isRegularFile)
            .filter(TextFileIndexer::isPlainTextFile)
            .forEach(file -> workerPool.submit(() -> {
                    tokenizer.indexFile(file).forEach(token -> store.add(token, file));
                }));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    private static boolean isPlainTextFile(Path path) {
        try {
            String type = Files.probeContentType(path);
            return type != null && "text/plain".equals(type);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Set<Path> search(String token) {
        return store.search(token);
    }

    @Override
    public void stop() {
        watcher.stop();
        watcherThread.interrupt();
        workerPool.shutdown();
    }

}
