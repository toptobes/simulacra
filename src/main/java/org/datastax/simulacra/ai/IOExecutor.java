package org.datastax.simulacra.ai;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class IOExecutor {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(Thread.ofVirtual().factory());

    public static ExecutorService get() {
        return EXECUTOR;
    }

    public static <U> CompletableFuture<U> defaultSupplyAsync(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(supplier, EXECUTOR);
    }
}
