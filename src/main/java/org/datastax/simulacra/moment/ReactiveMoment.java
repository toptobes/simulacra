package org.datastax.simulacra.moment;

import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.agents.Agent;
import org.datastax.simulacra.logging.HomemadeLogger;

import java.util.List;

import static org.datastax.simulacra.utils.Utils.asyncListProcessor;
import static org.datastax.simulacra.logging.HomemadeLogger.*;

public enum ReactiveMoment implements Moment {
    INSTANCE;

    @Override
    public boolean elapsesTime() {
        return true;
    }

    @Override
    public void elapse(List<Agent> agents) {
        log("ReactiveMoment.elapse");

        asyncListProcessor(agents)
            .onFail((error, list, offender) -> {
                error.printStackTrace();

                if (offender instanceof Agent agent) {
                    err("ReactiveMoment.elapse had a whoopsie", error);
                    agent.setCurrentAction(agent.getPlan().planForTheHour().get(SimClock.time().getHour() / SimClock.TIME_GRANULARITY));
                }
            })
            .discardIf(Agent::isInConversation)
            .peek(Agent::observe)
            .peek(Agent::planMoment)
            .discardIf(Agent::isInConversation)
            .peek(Agent::planPlace)
            .peek(Agent::reflect);

        HomemadeLogger.logActions(agents);
    }
}
