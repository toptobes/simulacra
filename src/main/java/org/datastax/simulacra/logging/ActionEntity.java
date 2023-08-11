package org.datastax.simulacra.logging;

import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.agents.Agent;

import java.time.Instant;
import java.time.ZoneOffset;

public record ActionEntity(
    String agentName,
    Instant runTime,
    Instant actionTime,
    String action
) {
    public static ActionEntity from(Agent agent) {
        return new ActionEntity(
            agent.getName(),
            SimClock.RUN_START_TIME,
            SimClock.time().toInstant(ZoneOffset.UTC),
            agent.getCurrentAction()
        );
    }
}
