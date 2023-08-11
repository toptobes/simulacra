package org.datastax.simulacra.moment;

import org.datastax.simulacra.Utils.AsyncListThreader;
import org.datastax.simulacra.agents.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.datastax.simulacra.Utils.asyncListProcessor;

public class MomentUtils {
    public static void withRetry(List<Agent> agents, Consumer<AsyncListThreader<Agent>> fn) {
        var retries = new ArrayList<Agent>();

        var initial = asyncListProcessor(agents)
            .onFail((error, list, offender) -> {
                if (offender instanceof Agent agent) {
                    retries.add(agent);
                }
            });

        fn.accept(initial);
        fn.accept(asyncListProcessor(retries));
    }
}
