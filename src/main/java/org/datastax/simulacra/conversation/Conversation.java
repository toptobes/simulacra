package org.datastax.simulacra.conversation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.utils.Utils;
import org.datastax.simulacra.agents.Agent;
import org.datastax.simulacra.ai.ChatService;
import org.datastax.simulacra.ai.FunctionClass;
import org.datastax.simulacra.ai.FunctionService;
import org.datastax.simulacra.memorystream.MemoryEntity;

import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.datastax.simulacra.logging.HomemadeLogger.*;
import static org.datastax.simulacra.utils.Utils.awaitAll;
import static org.datastax.simulacra.utils.Utils.useWriter;

public class Conversation {
    private static final PrintWriter writer = useWriter("simulacra/logs/_conversations.log");

    private String conversationHistory = "";

    private Supplier<String> instigatorPrelude;
    private Supplier<String> targetPrelude;

    private final Agent instigator;
    private final Agent target;

    private Agent speaker;

    private Conversation(Agent instigator, Agent target) {
        this.instigator = instigator;
        this.target = target;
        this.speaker = target;
    }

    public static CompletableFuture<Conversation> start(Agent instigator, Agent target) {
        instigator.setInConversation(true);
        target.setInConversation(true);

        instigator.setCurrentAction("conversing with " + target.getName());
        target.setCurrentAction("conversing with " + instigator.getName());

        var conversation = new Conversation(instigator, target);
        return conversation.constructPreludes().thenApply(v -> conversation);
    }

    public CompletableFuture<Boolean> converse() {
        speaker = (speaker == instigator) ? target : instigator;

        var prelude = (speaker == instigator) ? instigatorPrelude : targetPrelude;

        var prompt = """
          %s
          %s
          %s
          %s %s %s %s?
        """.formatted(
            prelude.get(),
            conversationHistory.isEmpty() ? "" : "Conversation history:",
            conversationHistory,
            conversationHistory.isEmpty() ? "What should" : "How would",
            speaker.getName(),
            conversationHistory.isEmpty() ? "say to" : "respond to",
            speaker == instigator ? target.getName() : instigator.getName()
        );

        return FunctionService.getDefault().query(prompt, ConversationResponse.class).thenApply(response -> {
            log(ANSI_BLUE, prelude.get());
            log(ANSI_PURPLE, response);

            conversationHistory += "%s: %s\n".formatted(speaker.getName(), response.response);
            return Boolean.TRUE.equals(response.shouldEnd);
        });
    }

    @FunctionClass
    private record ConversationResponse(
        @JsonPropertyDescription("The next message in the conversation")
        @JsonProperty(required = true)
        String response,
        @JsonPropertyDescription("Whether or not the conversation should end")
        Boolean shouldEnd
//        @JsonPropertyDescription("If the other agent needs to share an area for future plans, this is the area to share. Null otherwise.")
//        @JsonProperty(required = true)
//        String area2Share
    ) {}

    public CompletableFuture<Conversation> end() {
        var reflectionCfs = Stream.of(instigator, target).map(agent ->
            agent.reflectOverConversation(conversationHistory).thenAccept(v -> {
                agent.setInConversation(false);
                agent.setCurrentAction("Finishing conversation with " + (agent == instigator ? target.getName() : instigator.getName()));
            })
        ).toList();

        writer.println("""
            =====================================================================================================
            Conversation between %s and %s:
            %s
        """.formatted(
            instigator.getName(),
            target.getName(),
            conversationHistory
        ));

        return awaitAll(reflectionCfs).thenApply(v -> this);
    }

    private CompletableFuture<Void> constructPreludes() {
        return CompletableFuture.allOf(
            constructPrelude(this.instigator, this.target, true),
            constructPrelude(this.target, this.instigator, false)
        );
    }

    private CompletableFuture<Void> constructPrelude(Agent agent, Agent other, boolean isInstigator) {
        return getContextSummary(agent, other).thenAccept(summary -> {
            Supplier<String> prelude = () -> """
                %s
                It is %s
                Observation: %s
                Summary of relevant context from %s's memory: %s
                IT'S JUST RELEVANT CONTEXT, NOT ALL OF IT NEEDS TO/SHOULD BE USED.
                Please act casual and very human-like in your conversations, don't make them too long or formal.
                Don't repeat yourself or others too much unless necessary. Talk like you're a Gen-Z.
                Keep the conversation short and sweet, end it as soon as appropriate.
            """.formatted(
                agent.getSummary(),
                SimClock.timeString(),
                isInstigator ? agent.getLatestObservation() : (other.getName() + " is initiating a conversation with " + agent.getName()),
                other.getName(), summary
            );

            if (isInstigator) {
                instigatorPrelude = prelude;
            } else {
                targetPrelude = prelude;
            }
        });
    }

    private CompletableFuture<String> getContextSummary(Agent agent, Agent other) {
        return agent.getMemory().getMostRelevantMemories(agent.getName(), other.getName(), 5).thenCompose(memories -> {
            var prompt = """
                Summary about %s: %s
                Summarize the following statements into relevant context for a conversation with %s:
                %s
            """.formatted(
                agent.getName(),
                agent.getSummary(),
                other.getName(),
                Utils.map(memories, MemoryEntity::memory)
            );

            return ChatService.getDefault().query(prompt);
        });
    }

    public Agent getInstigator() {
        return instigator;
    }

    public Agent getTarget() {
        return target;
    }
}
