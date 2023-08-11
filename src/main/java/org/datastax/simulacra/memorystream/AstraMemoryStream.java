package org.datastax.simulacra.memorystream;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.datastax.simulacra.memorystream.MemoryUtils.calculateRelevanceAndSort;
import static org.datastax.simulacra.memorystream.MemoryUtils.getEmbeddings;

public class AstraMemoryStream implements MemoryStream {
    private final MemoryRepository repository = new MemoryRepository();

    @Override
    public void save(MemoryEntity memory) {
        save(memory.agentName(), List.of(memory));
    }

    @Override
    public void save(String name, List<MemoryEntity> memories) {
        repository.saveAll(memories);
    }

    @Override
    public CompletableFuture<List<MemoryEntity>> getMostRecentMemories(String name, int limit) {
        return repository.getRecentMemories(name, limit);
    }

    @Override
    public CompletableFuture<List<MemoryEntity>> getMostRelevantMemories(String name, String query, int limit) {
        return getMostRelevantMemories(name, List.of(query), limit).thenApply(l -> l.get(0));
    }

    @Override
    public CompletableFuture<List<List<MemoryEntity>>> getMostRelevantMemories(String name, List<String> queries, int limit) {
        return repository.getAllMemories(name)
            .thenCompose(memories ->
                getEmbeddings(queries).thenApply(embeddings -> calculateRelevanceAndSort(memories, embeddings, limit))
            );
    }
}
