package org.datastax.simulacra.logging;

import org.datastax.simulacra.agents.Agent;

import java.io.PrintWriter;
import java.util.List;

import static org.datastax.simulacra.utils.Utils.map;
import static org.datastax.simulacra.utils.Utils.useWriter;

public class HomemadeLogger {
    private static final ActionRepository repository = new ActionRepository();

    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_GRAY   = "\u001B[37m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_BLUE   = "\u001B[34m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_TEAL   = "\u001B[36m";
    public static final String ANSI_WHITE  = ANSI_RESET;

    private static final PrintWriter logWriter = useWriter("simulacra/logs/_debug.log", false);
    private static final PrintWriter errWriter = useWriter("simulacra/logs/_error.log", true);

    public static synchronized void log(String color, Object message) {
        logWriter.println(color + message + ANSI_RESET);
    }

    public static void log(Object message) {
        log(ANSI_GRAY, message);
    }

    public static void log(Agent agent, String color, Object message) {
        log(color, ANSI_TEAL + "[[AGENT]] " + agent.getName() + ": " + ANSI_RESET + message);
    }

    public static void log(Agent agent, Object message) {
        log(agent, ANSI_GRAY, message);
    }

    public static synchronized void err(String context, Throwable e) {
        errWriter.println(ANSI_TEAL + context + ": " + ANSI_RESET);
        e.printStackTrace(errWriter);
    }

    public static void err(Throwable e) {
        e.printStackTrace(errWriter);
    }

    public static void err(Object message) {
        errWriter.println(message);
    }

    public static synchronized void flushLogs() {
        logWriter.flush();
    }

    public static void logActions(List<Agent> agents) {
        repository.saveAll(map(agents, ActionEntity::from));
    }

    public static void logAction(Agent agent) {
        logActions(List.of(agent));
    }
}
