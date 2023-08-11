package org.datastax.simulacra.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.datastax.simulacra.Utils.groupBy;

public class AgentRegistry {
    public static final List<Agent> agents = new ArrayList<>();

    public static synchronized void register(Agent agent) {
        agents.add(agent);
    }

    public static synchronized Collection<List<Agent>> chunkedBySubarea() {
        return groupBy(agents, Agent::getSubarea).values();
    }
}
