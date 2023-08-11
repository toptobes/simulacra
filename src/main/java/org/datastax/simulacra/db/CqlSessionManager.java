package org.datastax.simulacra.db;

import com.datastax.oss.driver.api.core.CqlSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class CqlSessionManager {
    private static CqlSession session;

    public static synchronized CqlSession getSession() {
        if (session == null) {
            System.out.println("Initializing CqlSession...");
            session = buildCqlSession();
            DBInitializer.init(session);
        }
        return session;
    }

    public static void initialize() {
        getSession();
    }

    private static CqlSession buildCqlSession() {
        var clientID = System.getenv("ASTRA_CLIENT_ID");
        var clientSecret = System.getenv("ASTRA_CLIENT_SECRET");
        var keyspace = System.getenv("ASTRA_CLIENT_KEYSPACE");

        if (clientID == null || clientSecret == null || keyspace == null) {
            throw new RuntimeException("Missing environment variables. Please set ASTRA_CLIENT_ID, ASTRA_CLIENT_SECRET, and ASTRA_CLIENT_KEYSPACE.");
        }

        Path dir = Path.of("simulacra/secrets");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "secure-connect-*.zip")) {
            return CqlSession.builder()
                .withCloudSecureConnectBundle(stream.iterator().next())
                .withAuthCredentials(clientID, clientSecret)
                .withKeyspace(keyspace)
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Could not find secure-connect-*.zip in " + dir);
        }
    }
}
