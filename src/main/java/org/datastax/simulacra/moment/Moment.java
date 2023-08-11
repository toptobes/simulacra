package org.datastax.simulacra.moment;

import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.agents.Agent;

import java.util.List;

public sealed interface Moment permits ProactiveMoment, ReactiveMoment, ReflectiveMoment {
    void elapse(List<Agent> agents);
    boolean elapsesTime();

    // { reflected, planned }
    boolean[] ref = new boolean[] { false, false };

    static Moment forTime() {
        return
            (SimClock.time().getHour() == SimClock.START_TIME.getHour() && SimClock.time().getMinute() == 0 && (ref[0] = !ref[0]))
                ? ReflectiveMoment.INSTANCE :
            (SimClock.time().getMinute() == 0 && (ref[1] = !ref[1]))
                ? ProactiveMoment.INSTANCE
                : ReactiveMoment.INSTANCE;
    }
}
