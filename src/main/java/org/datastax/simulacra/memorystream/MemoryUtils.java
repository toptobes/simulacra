package org.datastax.simulacra.memorystream;

import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.ai.EmbeddingService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.min;
import static java.util.Collections.reverseOrder;
import static org.datastax.simulacra.utils.Utils.map;
import static org.datastax.simulacra.utils.Utils.zipMap;

public class MemoryUtils {
    public static CompletableFuture<List<List<Float>>> getEmbeddings(List<String> queries) {
        return EmbeddingService.getDefault().embed(queries);
    }

    public static List<List<MemoryEntity>> calculateRelevanceAndSort(List<MemoryEntity> memories, List<List<Float>> embeddings, int limit) {
        return map(embeddings, embedding -> calculateRelevance(memories, embedding).subList(0, min(limit, memories.size())));
    }

    public static List<MemoryEntity> calculateRelevance(List<MemoryEntity> memories, List<Float> embedding) {
        var rankings = map(memories, memory -> {
            var hourAccessed = memory.lastFetch();
            var hoursElapsed = SimClock.elapsed().toHours();
            var recency = hourAccessed * Math.pow(.99, hoursElapsed - hourAccessed);
            var relevance = testSimilarity(memory.embedding(), embedding);
            var importance = (double) memory.importance() / 10;
            return recency + importance + relevance;
        });

        return zipMap(memories, rankings, (memory, ranking) -> Map.entry(ranking, memory))
            .stream()
            .sorted(reverseOrder(Map.Entry.comparingByKey()))
            .map(Map.Entry::getValue)
            .toList();
    }

    public static double testSimilarity(List<Float> listA, List<Float> listB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < listA.size(); i++) {
            dotProduct += listA.get(i) * listB.get(i);
            normA += Math.pow(listA.get(i), 2);
            normB += Math.pow(listB.get(i), 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
