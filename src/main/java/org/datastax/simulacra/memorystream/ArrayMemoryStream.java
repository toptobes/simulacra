package org.datastax.simulacra.memorystream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.min;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.datastax.simulacra.memorystream.MemoryUtils.calculateRelevanceAndSort;
import static org.datastax.simulacra.memorystream.MemoryUtils.getEmbeddings;

public class ArrayMemoryStream implements MemoryStream {
    private final HashMap<String, List<MemoryEntity>> memories = new HashMap<>();

    @Override
    public void save(MemoryEntity memory) {
        save(memory.agentName(), List.of(memory));
    }

    @Override
    public void save(String name, List<MemoryEntity> memories) {
        this.memories.putIfAbsent(name, new ArrayList<>());
        this.memories.get(name).addAll(memories);
    }

    @Override
    public CompletableFuture<List<MemoryEntity>> getMostRecentMemories(String name, int limit) {
        return completedFuture(memories.get(name).subList(0, min(limit, memories.size())));
    }

    @Override
    public CompletableFuture<List<MemoryEntity>> getMostRelevantMemories(String name, String query, int limit) {
        return getMostRelevantMemories(name, List.of(query), limit).thenApply(l -> l.get(0));
    }

    @Override
    public CompletableFuture<List<List<MemoryEntity>>> getMostRelevantMemories(String name, List<String> queries, int limit) {
        return getEmbeddings(queries).thenApply(embeddings -> calculateRelevanceAndSort(memories.get(name), embeddings, limit));
    }
}
