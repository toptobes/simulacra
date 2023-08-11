package org.datastax.simulacra.ai;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface FunctionService {
    <T> CompletableFuture<T> query(String prompt, Class<T> response, Collection<?>...providers);

    static FunctionService getDefault() {
        return OpenAIService.INSTANCE;
    }
}
