package org.datastax.simulacra;

import org.datastax.simulacra.agents.AgentRegistry;
import org.datastax.simulacra.conversation.ConversationsManager;
import org.datastax.simulacra.conversation.ConversationsRegistry;
import org.datastax.simulacra.db.CqlSessionManager;
import org.datastax.simulacra.environment.WorldMap;
import org.datastax.simulacra.factory.SimFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Throwable {
        System.out.println(Instant.now());
        setUpDirectories();
        CqlSessionManager.initialize();

        SimFactory.loadSimulation(100);

        ConversationsManager.start();
        SimClock.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logRun();
            ConversationsManager.shutdown();
            SimClock.shutdown();
        }));

        while (!God.poll()) {
            SimClock.checkForError();
        }

        System.exit(0);
    }

    private static void logRun() {
        var log = Path.of("simulacra/logs/_runs.log");

        try {
            if (!Files.exists(log)) {
                Files.createFile(log);
            }

            Files.writeString(log, """
            ========================================
            Run start time: %s,
            Run ran for %d hours
             - %d agents,
             - %d places,
             - %d conversations
            """.formatted(
                SimClock.START_TIME,
                SimClock.elapsed().toHours(),
                AgentRegistry.agentCount(),
                WorldMap.GLOBAL.areaCount(),
                ConversationsRegistry.allConversations()
            ), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setUpDirectories() {
        List.of("simulacra", "simulacra/logs", "simulacra/cache").forEach(file -> {
            Path path = Path.of(file);

            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
