package org.datastax.simulacra.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EmbeddingService {
    CompletableFuture<List<Float>> embed(String text);
    CompletableFuture<List<List<Float>>> embed(List<String> text);

    static EmbeddingService getDefault() {
        return LocalEmbeddingService.INSTANCE;
    }
}
