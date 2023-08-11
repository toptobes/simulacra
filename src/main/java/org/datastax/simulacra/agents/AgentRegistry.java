package org.datastax.simulacra.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.datastax.simulacra.utils.Utils.groupBy;

public class AgentRegistry {
    private static final List<Agent> agents = new ArrayList<>();

    public static synchronized void register(Agent agent) {
        agents.add(agent);
    }

    public static synchronized Collection<List<Agent>> chunkedBySubarea() {
        return groupBy(agents, Agent::getSubarea).values();
    }

    public static synchronized Agent random() {
        return agents.get((int) (Math.random() * agents.size()));
    }

    public static synchronized int agentCount() {
        return agents.size();
    }
}
