package org.datastax.simulacra.db;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility annotation to mark a class as a repository and provide the SQL initializer.
 * The initializer is a semicolon-separated list of SQL statements.
 * <p>
 * Env vars can be used in the initializer using the syntax ${ENV_VAR_NAME}.
 * Default values can be provided w/ the syntax ${ENV_VAR_NAME:default_value}.
 * <p>
 * Annotated classes must still be manually added to the {@link DBInitializer#REPOSITORIES} list.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {
    String initializer();
    String[] format() default {};
}
