package org.datastax.simulacra.db;

import com.datastax.oss.driver.api.core.CqlSession;

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
        return CqlSession.builder()
            .withCloudSecureConnectBundle(Path.of("simulacra/secrets/secure-connect-simulacra.zip"))
            .withAuthCredentials(System.getenv("ASTRA_CLIENT_ID"), System.getenv("ASTRA_CLIENT_SECRET"))
            .withKeyspace("demo")
            .build();
    }
}
