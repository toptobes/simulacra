package org.datastax.simulacra;

import org.datastax.simulacra.conversation.ConversationsManager;
import org.datastax.simulacra.db.CqlSessionManager;
import org.datastax.simulacra.factory.SimFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Throwable {
        setUpDirectories();
        CqlSessionManager.initialize();

        SimFactory.loadSimulation(10);

        CqlSessionManager.initialize();
        ConversationsManager.start();
        SimClock.start();

        while (!God.poll()) {
            SimClock.checkForError();
        }

        ConversationsManager.shutdown();
        SimClock.shutdown();
        System.exit(0);
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
