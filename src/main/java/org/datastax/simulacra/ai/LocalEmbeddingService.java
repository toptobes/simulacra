package org.datastax.simulacra.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

import static org.datastax.simulacra.utils.Utils.map2list;
import static org.datastax.simulacra.ai.IOExecutor.supplyAsync;

public enum LocalEmbeddingService implements EmbeddingService {
    INSTANCE;

    private final HttpClient client = HttpClient.newHttpClient();
    private final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:5000/embed"))
        .header("Content-Type", "application/json");

    @Override
    public CompletableFuture<List<Float>> embed(String text) {
        return supplyAsync(() -> embedSync(List.of(text)).get(0));
    }

    @Override
    public CompletableFuture<List<List<Float>>> embed(List<String> text) {
        return supplyAsync(() -> embedSync(text));
    }

    private List<List<Float>> embedSync(List<String> text) {
        var body = new StringJoiner("\", \"", "{\"sentences\": [\"", "\"]}");

        for (String sentence : text) {
            body.add(sentence.replace("\"", "\\\""));
        }

        var bodyString = body.toString().replaceAll("\\s+", " ").trim();
        var request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyString)).build();

        try {
            var response = client.send(request, BodyHandlers.ofString());

            var result = response.body().replace("[[", "").replace("]]", "").split("],\\s*\\[");

            return map2list(result, s ->
                map2list(s.split(","), Float::parseFloat)
            );
        } catch (Exception e) {
            System.out.println(bodyString);
            throw new RuntimeException(e);
        }
    }
}
