package org.datastax.simulacra.memorystream;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.ai.EmbeddingService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.datastax.simulacra.Utils.aggregatePages;

public record MemoryEntity(
    String agentName,
    int createdAt,
    int lastFetch,
    int importance,
    byte memType,
    String memory,
    List<Float> embedding
) {
    public static CompletableFuture<MemoryEntity> from(String agentName, int importance, byte type, String memory) {
        var time = (int) SimClock.elapsed().toHours();

        return EmbeddingService.getDefault().embed(memory).thenApply(embedding -> new MemoryEntity(
            agentName, time, time, importance, type, memory, embedding
        ));
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    public static CompletableFuture<List<MemoryEntity>> from(AsyncResultSet resultSet) {
        return aggregatePages(resultSet).thenApply(rows ->
            rows.stream().map(row -> new MemoryEntity(
                row.getString("agent_name"),
                row.getInt("created_at"),
                row.getInt("last_fetch"),
                row.getInt("importance"),
                row.getByte("mem_type"),
                row.getString("memory"),
                ((CqlVector<Float>) row.get("embedding", CqlVector.class)).stream().toList()
            )).toList()
        );
    }
}
