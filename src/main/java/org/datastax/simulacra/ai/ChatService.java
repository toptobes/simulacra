package org.datastax.simulacra.ai;

import java.util.concurrent.CompletableFuture;

public interface ChatService {
    CompletableFuture<String> query(String prompt);

    static ChatService getDefault() {
        return OpenAIService.INSTANCE;
    }
}
