package org.datastax.simulacra.conversation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.datastax.simulacra.Utils.*;

@SuppressWarnings("InfiniteLoopStatement")
public class ConversationsManager {
    private static final ExecutorService exec = Executors.newSingleThreadExecutor();

    public static void start() {
        exec.submit(() -> {
            try {
                while (true) {
                    ConversationsRegistry.waitOnConversations();
                    processConversations();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public static void shutdown() {
        exec.shutdown();
    }

    private static void processConversations() {
        var snapshot = ConversationsRegistry.getSnapshot();

        var booleans = asyncListProcessor(snapshot)
            .pipe(Conversation::converse)
            .get();

        var cfs = zipMap(snapshot, booleans, (conversation, endOfConversation) -> (
            (endOfConversation)
                ? conversation.end().thenAccept(ConversationsRegistry::removeConversation)
                : CompletableFuture.<Void>completedFuture(null)
        ));

        awaitAll(cfs);
    }
}
