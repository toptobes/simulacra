package org.datastax.simulacra.moment;

import org.datastax.simulacra.agents.Agent;

import java.util.List;

import static org.datastax.simulacra.logging.HomemadeLogger.log;
import static org.datastax.simulacra.moment.MomentUtils.withRetry;

public enum ReflectiveMoment implements Moment {
    INSTANCE;

    @Override
    public boolean elapsesTime() {
        return false;
    }

    @Override
    public void elapse(List<Agent> agents) {
        log("ReflectiveMoment.elapse");

        withRetry(agents, (processor) ->
            processor.peek(Agent::synthesizeSummary)
        );
    }
}
