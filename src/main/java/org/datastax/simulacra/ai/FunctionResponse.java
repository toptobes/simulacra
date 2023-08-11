package org.datastax.simulacra.ai;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionResponse {
    String desc() default "";
}
