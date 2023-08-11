package org.datastax.simulacra.logging;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.datastax.simulacra.db.CqlSessionManager;
import org.datastax.simulacra.db.Repository;

import java.util.List;

@Repository(initializer = """
    CREATE TABLE IF NOT EXISTS actions (
        agent_name TEXT,
        run_time TIMESTAMP,
        action_time TIMESTAMP,
        action TEXT,
        PRIMARY KEY ((agent_name, run_time), action_time)
    ) WITH CLUSTERING ORDER BY (action_time DESC);
""")
public class ActionRepository {
    private final CqlSession session = CqlSessionManager.getSession();

    private final PreparedStatement insertMemory = session.prepare("""
        INSERT INTO actions
            (agent_name, run_time, action_time, action)
        VALUES
            (?, ?, ?, ?)
    """);

    public void saveAll(List<ActionEntity> actions) {
        actions.forEach(action -> {
            var boundInsertion = insertMemory.bind(
                action.agentName(), action.runTime(), action.actionTime(), action.action()
            );
            session.executeAsync(boundInsertion);
        });
    }
}
