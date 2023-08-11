package org.datastax.simulacra.memorystream;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MemoryStream {
    void save(MemoryEntity memory);

    void save(String name, List<MemoryEntity> memories);

    CompletableFuture<List<MemoryEntity>> getMostRelevantMemories(String name, String query, int limit);

    CompletableFuture<List<List<MemoryEntity>>> getMostRelevantMemories(String name, List<String> queries, int limit);

    CompletableFuture<List<MemoryEntity>> getMostRecentMemories(String name, int limit);
}
