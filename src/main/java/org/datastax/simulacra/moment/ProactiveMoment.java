package org.datastax.simulacra.moment;

import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.agents.Agent;

import java.time.LocalTime;
import java.util.List;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.datastax.simulacra.logging.HomemadeLogger.log;
import static org.datastax.simulacra.moment.MomentUtils.withRetry;

public enum ProactiveMoment implements Moment {
    INSTANCE;

    @Override
    public boolean elapsesTime() {
        return false;
    }

    @Override
    public void elapse(List<Agent> agents) {
        log("ProactiveMoment.elapse");

        withRetry(agents, (processor) ->
            processor
                .peek(agent ->
                    (agents.get(0).getPlan() == null || SimClock.time().toLocalTime().equals(LocalTime.MIDNIGHT))
                        ? agent.planDay()
                        : completedFuture(null)
                )
                .peek(agent -> agent.planRestOfHour(null))
        );
    }
}
