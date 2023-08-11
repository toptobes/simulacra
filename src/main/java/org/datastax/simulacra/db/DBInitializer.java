package org.datastax.simulacra.db;

import com.datastax.oss.driver.api.core.CqlSession;
import org.datastax.simulacra.logging.ActionRepository;
import org.datastax.simulacra.memorystream.MemoryRepository;

import java.util.Arrays;
import java.util.List;

public class DBInitializer {
    public static List<Class<?>> REPOSITORIES = List.of(
        MemoryRepository.class,
        ActionRepository.class
    );

    public static void init(CqlSession session) {
        try {
            REPOSITORIES.forEach((clazz) -> {
                var initializer = clazz.getAnnotation(Repository.class).initializer();

                Arrays.stream(initializer.split(";"))
                    .filter(s -> !s.isBlank())
                    .forEach(session::execute);
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
