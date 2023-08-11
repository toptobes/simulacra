package org.datastax.simulacra;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Math.min;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;
import static org.datastax.simulacra.logging.HomemadeLogger.ANSI_RED;
import static org.datastax.simulacra.logging.HomemadeLogger.log;

public class Utils {
    private Utils() {}

    public static <K, V> Map<K, V> associateBy(Collection<V> values, Function<V, K> transformer) {
        return values == null ? null : values.stream().collect(toMap(transformer, x -> x));
    }

    public static <M extends Map<K, V>, K, V> M associateByTo(M destination, List<V> values, Function<V, K> transformer) {
        if (values != null) {
            destination.putAll(values.stream().collect(toMap(transformer, x -> x)));
        }
        return destination;
    }

    public static <K, V> Map<K, V> associateWith(List<K> keys, Function<K, V> selector) {
        return keys == null ? null : keys.stream().collect(toMap(x -> x, selector));
    }

    public static <K, V> Map<K, List<V>> groupBy(List<V> values, Function<V, K> selector) {
        return values == null ? null : values.stream().collect(toMap(selector, List::of, Utils::cat));
    }

    @SuppressWarnings("unchecked")
    public static <A, B, R> List<R> zipMap(List<A> a, List<B> b, BiFunction<A, B, R> fn) {
        return IntStream.range(0, min(a.size(), b.size()))
            .mapToObj(i -> new Object[] { a.get(i), b.get(i) })
            .map(e -> fn.apply((A) e[0], (B) e[1]))
            .toList();
    }

    @SuppressWarnings("unchecked")
    public static <A, B> void zipIter(List<A> a, List<B> b, BiConsumer<A, B> fn) {
        IntStream.range(0, min(a.size(), b.size()))
            .mapToObj(i -> new Object[] { a.get(i), b.get(i) })
            .forEach(e -> fn.accept((A) e[0], (B) e[1]));
    }

    @SafeVarargs
    public static <E> List<E> cat(Collection<E> ...lists) {
        return Stream.of(lists).flatMap(Collection::stream).toList();
    }

    private static class ObjectMappers {
        public static final ObjectMapper JSON = new ObjectMapper();
        public static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    }

    public static <T> T readYaml(String path, TypeReference<T> ref) {
        try {
            return ObjectMappers.YAML.readValue(new File(path), ref);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readYaml(String path, TypeReference<T> ref, T orElse) {
        try {
            return ObjectMappers.YAML.readValue(new File(path), ref);
        } catch (IOException e) {
            return orElse;
        }
    }

    public static <T> T readJsonTree(String json, TypeReference<T> ref) {
        try {
            return ObjectMappers.JSON.readValue(json, ref);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode readJsonTree(String json) {
        try {
            return ObjectMappers.JSON.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeYaml(String path, Object yaml, boolean append) {
        try (var writer = new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8, append))) {
            ObjectMappers.YAML.writeValue(writer, yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeJsonAsString(Object json) {
        try {
            return ObjectMappers.JSON.writeValueAsString(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PrintWriter useWriter(String path) {
        return useWriter(path, true);
    }

    public static PrintWriter useWriter(String path, boolean autoFlush) {
        try {
            return new PrintWriter(new FileWriter(path), autoFlush);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String normalizeName(String name) {
        return name.trim().toLowerCase().replace(".", "");
    }

    public static CompletableFuture<List<Row>> aggregatePages(AsyncResultSet resultSet) {
        return aggregatePages(resultSet, new ArrayList<>());
    }

    private static CompletableFuture<List<Row>> aggregatePages(AsyncResultSet resultSet, List<Row> rows) {
        rows.addAll(
            StreamSupport.stream(resultSet.currentPage().spliterator(), false).toList()
        );

        return (resultSet.hasMorePages())
            ? resultSet
                .fetchNextPage()
                .thenCompose(result -> aggregatePages(result, rows))
                .toCompletableFuture()
            : completedFuture(rows);
    }

    public static <T> CompletableFuture<List<T>> awaitAll(List<CompletableFuture<T>> futures) {
        return completedFuture(
            map(futures, CompletableFuture::join)
        );
    }

    public static <T, R> List<R> map(Collection<T> list, Function<T, R> mapper) {
        return list.stream().map(mapper).toList();
    }

    public static <T> List<T> filter(Collection<T> list, Predicate<T> predicate) {
        return list.stream().filter(predicate).toList();
    }

    public static <T> List<T> filterNot(Collection<T> list, Predicate<T> predicate) {
        return filter(list, predicate.negate());
    }

    public static <T> List<T> flatten(Collection<? extends Collection<T>> list) {
        return list.stream().flatMap(Collection::stream).toList();
    }

    public static <T, R> List<R> map2list(T[] array, Function<T, R> mapper) {
        return Arrays.stream(array).map(mapper).toList();
    }

    public static <K, V> Map.Entry<K, V> getNthItem(LinkedHashMap<K, V> map, int n) {
        int i = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (i == n) {
                return entry;
            }
            i++;
        }
        return null;
    }

    public static String findClosestString(String original, Collection<String> strings) {
        return strings.stream()
            .min(Comparator.comparingDouble(str -> 1 - jaroWinklerSimilarity(original, str)))
            .orElse(null);
    }

    private static double jaroWinklerSimilarity(String s1, String s2) {
        var jaroDistance = jaroDistance(s1, s2);
        var prefixLength = commonPrefixLength(s1, s2);

        return jaroDistance + (prefixLength * 0.1 * (1 - jaroDistance));
    }

    private static double jaroDistance(String s1, String s2) {
        if (s1.equals(s2)) return 1;

        var matchDistance = Math.max(s1.length(), s2.length()) / 2 - 1;
        var matched1 = new boolean[s1.length()];
        var matched2 = new boolean[s2.length()];

        var matches = 0;
        var transpositions = 0;

        for (int i = 0; i < s1.length(); i++) {
            var start = Math.max(0, i - matchDistance);
            var end = Math.min(i + matchDistance + 1, s2.length());

            for (int j = start; j < end; j++) {
                if (matched2[j]) continue;
                if (s1.charAt(i) != s2.charAt(j)) continue;

                matched1[i] = true;
                matched2[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0;
        }

        for (int i = 0, k = 0; i < s1.length(); i++) {
            if (!matched1[i]) continue;

            while (!matched2[k]) {
                k++;
            }

            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        return (
            (double) matches / s1.length() +
            (double) matches / s2.length() +
            (double) (matches - transpositions / 2) / matches
        ) / 3.0;
    }

    private static int commonPrefixLength(String s1, String s2) {
        var len = Math.min(s1.length(), s2.length());

        var result = 0;
        for (int i = 0; i < len && s1.charAt(i) == s2.charAt(i); i++) {
            result++;
        }

        return Math.min(4, result);
    }

    public static Throwable getRootCause(Throwable e) {
        Throwable cause;

        while(null != (cause = e.getCause())  && (e != cause) ) {
            e = cause;
        }
        return e;
    }

    public static String substringBefore(String str, String separator) {
        if (str == null || str.isEmpty()) {
            return str;
        } else if (separator.isEmpty()) {
            return "";
        }

        var pos = str.indexOf(separator);
        return (pos == -1) ? str : str.substring(0, pos);
    }

    public static <T> AsyncListThreader<T> asyncListProcessor(List<T> list) {
        return new AsyncListThreader<>(new ArrayList<>(list), AsyncListThreader.ErrorHandler.NOOP);
    }

    public record AsyncListThreader<T>(List<T> list, ErrorHandler onFail) {
        public AsyncListThreader<T> onFail(ErrorHandler fn) {
            return new AsyncListThreader<>(list, fn);
        }

        public <R> AsyncListThreader<R> pipeSync(Function<T, R> fn) {
            return new AsyncListThreader<>(map(list, fn), onFail);
        }

        public AsyncListThreader<T> peekSync(Consumer<T> fn) {
            list.forEach(fn);
            return this;
        }

        public <R> AsyncListThreader<R> pipe(Function<T, CompletableFuture<R>> fn) {
            return new AsyncListThreader<>(removeErrors(getMapped(fn)), onFail);
        }

        public <R> AsyncListThreader<T> peek(Function<T, CompletableFuture<R>> fn) {
            var newList = zipMap(list, getMapped(fn), (o, n) -> {
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
            return map(list, fn);
        }

        public List<T> get() {
            return list;
        }

        private <R> List<?> getMapped(Function<T, CompletableFuture<R>> fn) {
            var futures = map(list, fn);

            return zipMap(list, futures, (l, f) -> {
                try {
                    return f.get();
                } catch (Throwable e) {
                    log(ANSI_RED, "Failed");
                    e.printStackTrace();
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
            ErrorHandler NOOP = (e, l, o) -> {};
        }

        private static class Errored {}
    }

    public static <A, B> Pair<A, B> cons(A a, B b) {
        return new Pair<>(a, b);
    }

    public record Pair<A, B>(A fst, B snd) {}

    public static class Map2ListSerializer extends JsonSerializer<Map<?, ?>> {
        @Override
        public void serialize(Map<?, ?> itemsMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            List<?> itemsList = new ArrayList<>(itemsMap.values());
            jsonGenerator.writeObject(itemsList);
        }
    }
}
