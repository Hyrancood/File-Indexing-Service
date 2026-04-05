package com.shapiro.interfaces;

import java.nio.file.Path;
import java.util.Set;

public interface Indexer {
    
    void addPath(Path path);

    void removePath(Path path);
    
    Set<Path> search(String token);
    
    void stop();
}
