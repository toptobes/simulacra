package org.datastax.simulacra.memorystream;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.datastax.simulacra.db.CqlSessionManager;
import org.datastax.simulacra.db.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository(initializer = """
    CREATE TABLE IF NOT EXISTS memories_by_agent (
        agent_name TEXT,
        created_at INT,
        memory_id UUID,
        last_fetch INT,
        importance INT,
        mem_type TINYINT,
        memory TEXT,
        embedding VECTOR<FLOAT, 768>,
        PRIMARY KEY (agent_name, created_at, memory_id)
    ) WITH CLUSTERING ORDER BY (created_at DESC, memory_id ASC);
    
    CREATE CUSTOM INDEX IF NOT EXISTS memories_ann ON memories_by_agent(embedding)
        USING 'StorageAttachedIndex';
        
    TRUNCATE TABLE memories_by_agent;
""")
public class MemoryRepository {
    private final CqlSession session = CqlSessionManager.getSession();

    private final PreparedStatement insertMemory = session.prepare("""
        INSERT INTO memories_by_agent
            (agent_name, created_at, memory_id, last_fetch, importance, mem_type, memory, embedding)
        VALUES
            (?, ?, ?, ?, ?, ?, ?, ?)
    """);

    public void saveAll(List<MemoryEntity> memories) {
        memories.forEach(memory -> {
            var boundInsertion = insertMemory.bind(
                memory.agentName(), memory.createdAt(), UUID.randomUUID(), memory.lastFetch(), memory.importance(), memory.memType(), memory.memory(), CqlVector.newInstance(memory.embedding())
            );
            session.executeAsync(boundInsertion);
        });
    }

    private final PreparedStatement recentMemories = session.prepare("""
        SELECT * FROM memories_by_agent WHERE agent_name = ? ORDER BY created_at DESC LIMIT ?
    """);

    public CompletableFuture<List<MemoryEntity>> getRecentMemories(String agentName, int limit) {
        var boundQuery = recentMemories.bind(agentName, limit);
        var resultSet = session.executeAsync(boundQuery);
        return resultSet.toCompletableFuture().thenCompose(MemoryEntity::from);
    }

    private final PreparedStatement allMemories = session.prepare("""
        SELECT * FROM memories_by_agent WHERE agent_name = ?
    """);

    public CompletableFuture<List<MemoryEntity>> getAllMemories(String agentName) {
        var boundQuery = allMemories.bind(agentName);
        var resultSet = session.executeAsync(boundQuery);
        return resultSet.toCompletableFuture().thenCompose(MemoryEntity::from);
    }

    private final PreparedStatement similarMemories = session.prepare("""
        SELECT * FROM memories_by_agent WHERE agent_name = ? ORDER BY embedding ANN OF ? LIMIT ?
    """);

    public CompletableFuture<List<MemoryEntity>> getSimilarMemories(String agentName, List<Float> query, int limit) {
        var boundQuery = similarMemories.bind(agentName, query, limit);
        var resultSet = session.executeAsync(boundQuery);
        return resultSet.toCompletableFuture().thenCompose(MemoryEntity::from);
    }
}
