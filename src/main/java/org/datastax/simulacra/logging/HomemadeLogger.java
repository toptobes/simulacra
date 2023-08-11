package org.datastax.simulacra.logging;

import org.datastax.simulacra.agents.Agent;

import java.io.PrintWriter;
import java.util.List;

import static org.datastax.simulacra.Utils.map;
import static org.datastax.simulacra.Utils.useWriter;

public class HomemadeLogger {
    private static final ActionRepository repository = new ActionRepository();

    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_GRAY   = "\u001B[37m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_BLUE   = "\u001B[34m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_TEAL   = "\u001B[36m";
    public static final String ANSI_WHITE  = ANSI_RESET;

    private static final PrintWriter writer = useWriter("simulacra/logs/_debug.log", false);

    public static void log(String color, Object message) {
//        System.out.println(color + message + ANSI_RESET);
        writer.println(color + message + ANSI_RESET);
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

    public static void flush() {
        writer.flush();
    }

    public static void logActions(List<Agent> agents) {
        repository.saveAll(map(agents, ActionEntity::from));
    }
}
