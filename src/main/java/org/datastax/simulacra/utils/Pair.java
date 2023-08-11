package org.datastax.simulacra.utils;

/**
 * Why doesn't Java have at least a Pair/Cons class???
 */
public record Pair<A, B>(A fst, B snd) {
    public static <A, B> Pair<A, B> cons(A a, B b) {
        return new Pair<>(a, b);
    }
}
