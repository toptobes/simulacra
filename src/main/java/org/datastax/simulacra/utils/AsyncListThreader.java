package org.datastax.simulacra.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public record AsyncListThreader<T>(List<T> list, ErrorHandler onFail) {
    public AsyncListThreader<T> onFail(ErrorHandler fn) {
        return new AsyncListThreader<>(list, fn);
    }

    public <R> AsyncListThreader<R> pipeSync(Function<T, R> fn) {
        return new AsyncListThreader<>(Utils.map(list, fn), onFail);
    }

    public AsyncListThreader<T> peekSync(Consumer<T> fn) {
        list.forEach(fn);
        return this;
    }

    public <R> AsyncListThreader<R> pipe(Function<T, CompletableFuture<R>> fn) {
        return new AsyncListThreader<>(removeErrors(getMapped(fn)), onFail);
    }

    public <R> AsyncListThreader<T> peek(Function<T, CompletableFuture<R>> fn) {
        var newList = Utils.zipMap(list, getMapped(fn), (o, n) -> {
            if (n == Errored.class) {
                return n;
            }
            return o;
        });

        return new AsyncListThreader<>(removeErrors(newList), onFail);
    }

    public AsyncListThreader<T> discardIf(Predicate<T> fn) {
        return new AsyncListThreader<>(list.stream().filter(fn.negate()).toList(), onFail);
    }

    public <R> List<R> get(Function<T, R> fn) {
        return Utils.map(list, fn);
    }

    public List<T> get() {
        return list;
    }

    private <R> List<?> getMapped(Function<T, CompletableFuture<R>> fn) {
        var futures = Utils.map(list, fn);

        return Utils.zipMap(list, futures, (l, f) -> {
            try {
                return f.get();
            } catch (Throwable e) {
                onFail.onError(e, list, l);
                return Errored.class;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <R> List<R> removeErrors(List<?> list) {
        return (List<R>) list.stream().filter(x -> x != Errored.class).toList();
    }

    public interface ErrorHandler {
        void onError(Throwable error, List<?> list, Object offender);

        ErrorHandler NOOP = (e, l, o) -> {
        };
    }

    private static class Errored {
    }
}
