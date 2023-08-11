package org.datastax.simulacra.utils;

public record Pair<A, B>(A fst, B snd) {
    public static <A, B> Pair<A, B> cons(A a, B b) {
        return new Pair<>(a, b);
    }
}
