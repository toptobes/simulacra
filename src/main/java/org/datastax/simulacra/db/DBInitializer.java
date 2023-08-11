package org.datastax.simulacra.db;

import com.datastax.oss.driver.api.core.CqlSession;
import org.datastax.simulacra.logging.ActionRepository;
import org.datastax.simulacra.memorystream.MemoryRepository;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    .map(DBInitializer::replaceEnvVars)
                    .filter(s -> !s.isBlank())
                    .forEach(session::execute);
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String replaceEnvVars(String sql) {
        var pattern = Pattern.compile("\\$\\{([^:}]+)(?::([^}]+))?}");
        var matcher = pattern.matcher(sql);
        var result = new StringBuilder();

        while (matcher.find()) {
            String envVarName = matcher.group(1);
            String defaultValue = matcher.group(2);
            String replacement = System.getenv(envVarName);

            if (replacement == null) {
                if (defaultValue != null) {
                    replacement = defaultValue;
                } else {
                    throw new IllegalArgumentException("Environment variable " + envVarName + " not found and no default value provided.");
                }
            }

            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
