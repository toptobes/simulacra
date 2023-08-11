package org.datastax.simulacra.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.datastax.simulacra.SimClock;
import org.datastax.simulacra.ai.ChatService;
import org.datastax.simulacra.ai.EnumType;
import org.datastax.simulacra.ai.FunctionResponse;
import org.datastax.simulacra.ai.FunctionService;
import org.datastax.simulacra.conversation.ConversationsRegistry;
import org.datastax.simulacra.environment.Area;
import org.datastax.simulacra.environment.SubArea;
import org.datastax.simulacra.environment.WorldMap;
import org.datastax.simulacra.memorystream.MemoryEntity;
import org.datastax.simulacra.memorystream.MemoryStream;
import org.datastax.simulacra.memorystream.MemoryType;
import org.datastax.simulacra.memorystream.Plan;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.datastax.simulacra.utils.Utils.*;
import static org.datastax.simulacra.logging.HomemadeLogger.*;

public class Agent {
    private final String name;
    private final Integer age;
    private final List<String> traits;
    private final String circadianRhythm;
    private final MemoryStream memory;
    private final Area house;

    private final WorldMap localMap = WorldMap.GLOBAL;

    private Plan plan;
    private String summary;
    private SubArea subarea;
    private String currentAction;
    private List<String> latestObservations;
    private int importanceCounter = 0;
    private boolean isInConversation = false;

    public Agent(
        String name,
        Integer age,
        List<String> traits,
        MemoryStream memory,
        SubArea subarea,
        String currentAction,
        String circadianRhythm,
        Area house
    ) {
        this.age = age;
        this.name = name;
        this.house = house;
        this.traits = traits;
        this.memory = memory;
        this.subarea = subarea;
        this.currentAction = currentAction;
        this.circadianRhythm = circadianRhythm;
        subarea.register(this);
    }

    public CompletableFuture<Void> planDay() {
        log(this, "Planning " + name + "'s day...");

        var yesterdaysPlan = (plan == null) ? "'s plan is unknown, but you can guess something reasonable" : plan;

        var prompt = """
                Keep each plan 10 words or less.
                Format example `8am) Wake up and do morning routine`
                Keep in mind occupations and day of week.
                %s.
                Rough circadian rhythm: %s.
                Yesterday, %s, %s %s.
                Current action %s.
                Do not include other people in your plans, not even friends, family, or community.
                Today is %s. Respond with very board strokes plan for every hour of the waking day.
                (newline separated)
            """.formatted(
            summary,
            circadianRhythm,
            SimClock.dateString(-1), name, yesterdaysPlan,
            currentAction,
            SimClock.dateString()
        );

        return ChatService.getDefault().query(prompt).thenAccept(dailyPlan ->
            plan = new Plan(dailyPlan)
        );
    }

    public CompletableFuture<Void> rePlanDay(String reason) {
        log(this, "Re-planning " + name + "'s day...");

        var time = SimClock.time();

        var nextHour = (time.getMinute() > 0)
            ? time.truncatedTo(ChronoUnit.HOURS).plusHours(1).getHour()
            : time.getHour();

        var prompt = """
            Keep each plan 10 words or less.
            Format example `8am) Wake up and do morning routine`
            Keep in mind occupations and day of week.
            %s.
            Rough circadian rhythm: %s.
            The previous plan for the waking hour of today, %s, was %s.
            Current action %s.
            Context for re-planning %s.
            Do not include other people in your plans, not even friends of family.
            Never include others in your plans, not even friends, family, or community, UNLESS THERE'S REASON TO DO SO IN THE CONTEXT.
            The hour today is %02d:00. Respond with very board strokes plan for every hour of the waking day.
            KEEP THE PLANS FROM PREVIOUS HOURS EXACTLY THE SAME.
            (newline separated)
        """.formatted(
            summary,
            circadianRhythm,
            SimClock.dateString(), plan.planForTheDay(),
            currentAction,
            reason,
            nextHour
        );

        return ChatService.getDefault().query(prompt).thenAccept(dailyPlan ->
            log(this, ANSI_YELLOW, "New plan: " + (plan = new Plan(dailyPlan)))
        ).thenCompose(v -> planRestOfHour(reason));
    }

    @FunctionResponse
    private record HourlyPlan(
        @JsonProperty(required = true)
        String minute00,
        @JsonProperty(required = true)
        String minute10,
        @JsonProperty(required = true)
        String minute20,
        @JsonProperty(required = true)
        String minute30,
        @JsonProperty(required = true)
        String minute40,
        @JsonProperty(required = true)
        String minute50
    ) {}

    public CompletableFuture<Void> planRestOfHour(String context) {
        var dailyPlan = plan.planForTheDay();

        var hour = (SimClock.time().getHour()) > 12
            ? SimClock.time().getHour() - 12
            : SimClock.time().getHour();

        var suffix = (SimClock.time().getHour()) >= 12
            ? "pm"
            : "am";

        var times = IntStream.range(0, 60 / SimClock.TIME_GRANULARITY)
            .mapToObj(i -> "%d:%02d%s".formatted(hour, i * SimClock.TIME_GRANULARITY, suffix))
            .toList();

        var prompt = """
            Keep each plan 30 words or less
            Format example `7:00am) Get out of bed and go to bathroom`
            Keep in mind occupations and day of week
            %s
            %s
            Keep the plans consistent with the plan for this hour, don't diverge from it at all, just expound. Be very descriptive.
            Never include others in your plans, not even friends, family, or community%s
            Broad plan for the hour: %s
            The hour today is %s. Respond with his exact, expounded upon plan for the hour, based on the current plan, and keep in mind the plan for the next hour as well
            Write the plans for ALL of the following times: %s
        """.formatted(
            summary,
            (context == null) ? "" : ("Context for re-planning " + context + "."),
            dailyPlan.get(SimClock.time().getHour()),
            (context == null) ? "" : ", UNLESS THERE'S REASON TO DO SO IN THE CONTEXT",
            SimClock.timeString(),
            times
        );

        return FunctionService.getDefault().query(prompt, HourlyPlan.class).thenAccept(plan -> {
            var hourlyPlan = List.of(plan.minute00, plan.minute10, plan.minute20, plan.minute30, plan.minute40, plan.minute50);
            this.plan = new Plan(dailyPlan, hourlyPlan);
            log(this, ANSI_YELLOW, "Hourly plan: " + String.join("\n", hourlyPlan));
        }).exceptionallyCompose(e -> {
            err("Failed to create hourly plan", e);
            return planRestOfHour(context);
        });
    }

    public CompletableFuture<Void> observe() {
        var subareaItemsSummary = subarea.items().values().stream().map(i -> i.name() + " is " + i.status()).toList();
        var subareaAgentsSummary = subarea.agents().values().stream().map(i -> i.name + " is " + i.currentAction).toList();
        var observations = cat(subareaItemsSummary, subareaAgentsSummary);
        return synthesizeObservation(observations.toString());
    }

    @FunctionResponse
    private record MomentPlan(
        @JsonPropertyDescription("The action the agent should be doing for the next " + SimClock.TIME_GRANULARITY + " minutes")
        @JsonProperty(required = true)
        String action,

        @JsonPropertyDescription("Reason for re-planning or null if no replan needed. Only replan if absolutely necessary (e.g., if the agent is straying far enough from the plan)")
        String replanReason,

        @EnumType
        @JsonPropertyDescription("The name of the person you want to talk to, or null if no talking desired")
        String talkTo,

        @EnumType
        @JsonPropertyDescription("List of each items to update if interacting with them (don't force any interaction, and pay attention to their current status before using it). DO NOT FORGET TO RESET THE ITEMS TO SOME IDLE STATUS AFTER YOU'RE FINISHED INTERACTING WITH THEM.")
        List<String> items,

        @JsonPropertyDescription("The new corresponding statuses of each item, or a neutral status such as \"idle\" or \"empty\" if done")
        List<String> correspondingItemStatuses
    ) {}

    public CompletableFuture<Void> planMoment() {
        return memory.getMostRelevantMemories(name, latestObservations, 3).thenCompose(memories -> {
            var flatMemories = memories.stream().flatMap(Collection::stream).distinct().map(MemoryEntity::memory).toList();

            var prompt = """
                %s
                Never include others in your plans, not even friends, family, or community, unless there is reason to do so in the hourly plan.
                Try to be a bit descriptive with your moment plan.
                The action should be feasible for %d minutes.
                Previous action %s.
                Observations: %s.
                The items in the room and their statuses are: %s
                Base %s's next action on his plan, but also on his observations and context.
                A re-plan of the day is only needed if the action strays far enough from the plan to warrant one.
                Relevant context: %s.
                Plan for the hour: %s.
                Plan for right now: %s.
                The exact time is %s.
                Try not to repeat yourself unless it makes sense in context (such as sleeping)
                The plan should be realistic, descriptive, and unique
            """.formatted(
                summary,
                SimClock.TIME_GRANULARITY,
                currentAction,
                flatMemories,
                subarea.items().values().stream().map(i -> '"' + i.name() + "\" ( is " + i.status() + ")").toList(),
                name,
                latestObservations,
                plan.planForTheHour(),
                plan.planForTheHour().get(SimClock.time().getMinute() / SimClock.TIME_GRANULARITY),
                SimClock.dateTimeString()
            );

            return FunctionService.getDefault().query(prompt, MomentPlan.class, subarea.agentNamesExcluding(name), subarea.itemNames()).thenCompose(response -> {
                log(this, ANSI_WHITE, "Plan 4 moment for " + name + ": " + response);

                if (response.talkTo != null) {
                    var agent = subarea.findAgent(response.talkTo);

                    if (agent != null) {
                        ConversationsRegistry.addConversation(this, agent);
                        return completedFuture(null);
                    }
                }

                log(this, "Items to update:" + response.items);
                log(this, subarea.items().keySet());

                if (response.items != null) {
                    zipForEach(response.items, response.correspondingItemStatuses, subarea::findAndUpdateItem);
                }

                currentAction = response.action;

                return (response.replanReason == null)
                    ? completedFuture(null)
                    : rePlanDay(response.replanReason);
            });
        });
    }

    @FunctionResponse
    private record MomentPlanV2(
        @JsonPropertyDescription("Whether or not the agent should react to the observation")
        @JsonProperty(required = true)
        boolean shouldReact,

        @JsonPropertyDescription("The appropriate reaction to the observation, or null if no reaction desired")
        String reaction,

        @EnumType
        @JsonPropertyDescription("The name of the person you want to talk to b/c of the observations, or null if no talking desired")
        String talkTo
    ) {}

    public CompletableFuture<Void> planMomentV2() {
        var queries = latestObservations.stream()
            .flatMap(o -> Stream.of("What is " + name + "'s relationship with " + o + "?", o))
            .toList();

        return memory.getMostRelevantMemories(name, queries, 3).thenCompose(memories -> {
            var context = memories.stream().flatMap(Collection::stream).distinct().map(MemoryEntity::memory).toList();

            var prompt = """
                %s
                The time is %s.
                %s's previous action was %s.
                Summary of relevant context from their memory: %s.
                Latest observations: %s.
                Should they react to any of the observations? And if so, what is an appropriate reaction?
                ONLY REACT DIRECTLY TO THE EXACT OBSERVATIONS AND NOTHING ELSE
            """.formatted(
                summary,
                SimClock.dateTimeString(),
                name, currentAction,
                latestObservations,
                context
            );

            return FunctionService.getDefault().query(prompt, MomentPlanV2.class, subarea.agentNamesExcluding(name), subarea.itemNames()).thenCompose(response -> {
                log(this, ANSI_WHITE, "Plan 4 moment for " + name + ": " + response);

                if (response.talkTo != null) {
                    var agent = subarea.findAgent(response.talkTo);

                    if (agent != null) {
                        ConversationsRegistry.addConversation(this, agent);
                        return completedFuture(null);
                    }
                }

                currentAction = plan.planForTheHour().get(SimClock.time().getMinute() / SimClock.TIME_GRANULARITY);

                return (response.talkTo != null || response.reaction == null)
                    ? completedFuture(null)
                    : rePlanDay(response.reaction);
            });
        });
    }

    @FunctionResponse
    private record PlanPlace(
        @EnumType
        @JsonProperty(required = true)
        String placeName
    ) {}

    public CompletableFuture<Void> planPlace() {
        var areaPrompt = """
            %s is currently in %s.
            %s knows of the following areas: %s.
            %s's house is %s.
            %s is planning to %s.
            Which name is most appropriate for %s to go to to execute his plan?
            Prefer to stay in the same area if the action can be accomplished there.
            Make sure the JSON is valid format.
        """.formatted(
            name, subarea.area().name(),
            name, localMap.areaNames(),
            name, house.name(),
            name, currentAction,
            name
        );

        return FunctionService.getDefault().query(areaPrompt, PlanPlace.class, localMap.areaNames()).thenCompose(areaName -> {
            log(this, "Area name: " + areaName.placeName);
            var foundArea = localMap.findArea(areaName.placeName);
            var area = (foundArea != null) ? foundArea : subarea.area();

            var subareaPrompt = """
                %s is currently in %s, which contains the subareas %s.
                %s is planning to %s.
                Which subarea is most appropriate for %s to go to?
                Make sure the JSON is valid format.
            """.formatted(
                name, area.name(), area.subareaNames(),
                name, currentAction,
                name
            );

            return FunctionService.getDefault().query(subareaPrompt, PlanPlace.class, area.subareaNames()).thenAccept(subareaName -> {
                log(this, "Place 4 plan: " + subareaName.placeName);
                var foundSubarea = area.findSubarea(subareaName.placeName);

                subarea.unregister(this);
                subarea = (foundSubarea != null) ? foundSubarea : subarea;
                subarea.register(this);
            });
        });
    }

    @FunctionResponse
    private record ReflectionQuestions(
        String q1,
        String q2,
        String q3
    ) {}

    public CompletableFuture<Void> reflect() {
        log(this, "Importance counter: " + importanceCounter);
        if (importanceCounter < 200) {
            return completedFuture(null);
        }
        importanceCounter = 0;

        var ref = new Object() {
            int i = 0;
        };

        return memory.getMostRecentMemories(name, 75).thenCompose(memories -> {
            var statements = map(memories, e -> (
                (ref.i++) + ") " + e.memory()
            ));

            var questionsPrompt = """
            %s
            
            Given only the information above, what are 3 most salient high-level questions we can answer about the subjects
            in the statements?
        """.formatted(statements);

            return FunctionService.getDefault().query(questionsPrompt, ReflectionQuestions.class).thenCompose(questions -> {
                var answers = asyncListProcessor(List.of(questions.q1, questions.q2, questions.q3))
                    .pipe(question -> (
                        memory.getMostRelevantMemories(name, question, 5)
                    ))
                    .get(xs -> (
                        map(xs, MemoryEntity::memory)
                    ));

                var insightsPrompt = """
                    Statements about %s:
                    %s
                    
                    What 5 high-level insights can you infer from the above statements?
                    Example format: `insight (because of <write statement #1 here>, <statement #4>, <statement #5>, etc)`
                """.formatted(name, answers);

                return ChatService.getDefault().query(insightsPrompt).thenCompose(insights ->
                    MemoryEntity.from(name, 7, MemoryType.REFLECTION, insights).thenAccept(memory::save)
                );
            });
        });
    }

    public CompletableFuture<Void> reflectOverConversation(String conversationHistory) {
        return synthesizeObservation(conversationHistory);
    }

    public CompletableFuture<Void> synthesizeSummary() {
        log(this, "Summarizing " + name + "'s memories...");

        var queries = List.of(
            "How would one describe " + name + "’s core characteristics given the following statements?",
            "How would one describe " + name + "’s current daily occupation given the following statements?",
            "How would one describe " + name + "’s feeling about his recent progress in life given the following statements?"
        );

        var prompts = List.of(
            "Being brief (LESS THAN 30 WORDS) very abstractly describe " + name + "’s important core characteristics given the following statements:",
            "Being brief (LESS THAN 30 WORDS) very abstractly describe " + name + "’s important current daily occupation given the following statements:",
            "Being brief (LESS THAN 30 WORDS) very abstractly describe " + name + "’s important feeling about his recent progress in life given the following statement:"
        );

        return memory.getMostRelevantMemories(name, queries, 10).thenCompose(memories -> {
            var resultsFuture = zipMap(prompts, memories, (p, m) -> (
                ChatService.getDefault().query(p + m.stream().map(MemoryEntity::memory).toList())
            ));

            return awaitAll(resultsFuture).thenAccept(results ->
                summary = "Name: %s; Age: %s; Traits: %s; %s".formatted(name, age, traits, results)
            );
        });
    }

    @FunctionResponse
    private record Observation(
        @JsonPropertyDescription("The synthesized, actionable, observation")
        @JsonProperty(required = true)
        String observation,

        @JsonPropertyDescription("The non-inflated, conservative importance of the observation. Be realistic, don't try to draw out meaning. It should be based on importance to remember, be conservative.")
        @JsonProperty(required = true)
        int importance
    ) {}

    private CompletableFuture<Void> synthesizeObservation(String context) {
        var prompt = """
            Agent summary:
            %s
            Synthesize the following into something more coherent and important to the agent (perhaps that they might want to act upon):
            %s
            With a single number, rate the importance of **remembering** the observation on a scale of 1 to 10,
            where 1 is purely mundane (e.g., brushing teeth, making bed)
            and 10 is extremely poignant (e.g., a break up, college acceptance),
            of course with respect to the agent.
            Must be an integer.
            
            The observation should be succinct and to the point, as well as actionable if applicable.
            DO NOT DRAMATIZE THE OBSERVATION OR RATING. KEEP IT CONSERVATIVE AND FACTUAL.
            THE OBSERVATIONS SHOULD BE ABOUT HIS SURROUNDINGS AND OTHER PEOPLE, NOT ABOUT HIMSELF W/ THE AGENT SUMMARY
            
            Something like "The coffee table is idle, the sofa is being used by all family members, and the TV is
            being used by Michael. Jane Smith is reviewing progress and making adjustments. John Smith is starting
            working on priority tasks. Michael Smith is continuing to work on artwork and letting creativity
            flow freely." is NOT important because it doesn't build anything about the agent's character.
        """.formatted(summary, context);

        return FunctionService.getDefault().query(prompt, Observation.class).thenCompose(response -> {
            log(this, response);
            latestObservations = List.of(response.observation);
            importanceCounter += response.importance;

            log(this, "Observation: " + response.observation);
            log(this, "Importance: " + response.importance);

            return MemoryEntity.from(name, response.importance, MemoryType.OBSERVATION, response.observation)
                .thenAccept(memory::save);
        });
    }

    @FunctionResponse
    private record ObservationV2(
        @JsonPropertyDescription("The non-inflated, conservative importance of each observation. Be realistic, don't try to draw out meaning. It should be based on importance to remember, be conservative.")
        @JsonProperty(required = true)
        List<Integer> importances
    ) {}

    private CompletableFuture<Void> synthesizeObservationV2(List<String> observations) {
        var prompt = """
            Agent summary:
            %s
            Observations:
            %s
            With a single number, rate each importance of **remembering** the observation on a scale of 1 to 10,
            where 1 is purely mundane (e.g., brushing teeth, making bed)
            and 10 is extremely poignant (e.g., a break up, college acceptance),
            of course with respect to the agent.
            Must be an integer.
            DO NOT DRAMATIZE THE OBSERVATION OR RATING. KEEP IT CONSERVATIVE AND FACTUAL.
            THE OBSERVATIONS SHOULD BE ABOUT HIS SURROUNDINGS AND OTHER PEOPLE, NOT ABOUT HIMSELF W/ THE AGENT SUMMARY
            DO NOT ADD COMMENTS TO THE RATINGS, JUST DO THE RATINGS
        """.formatted(summary, observations);

        return FunctionService.getDefault().query(prompt, ObservationV2.class).thenCompose(response -> {
            log(this, response);
            latestObservations = observations;

            var entities = zipMap(observations, response.importances, (o, i) -> (
                MemoryEntity.from(name, i, MemoryType.OBSERVATION, o)
            ));

            return awaitAll(entities).thenAccept(memories ->
                memory.save(name, memories)
            );
        });
    }

    // -- PROPERTY ACCESSORS --

    public String getName() {
        return name;
    }

    public MemoryStream getMemory() {
        return memory;
    }

    public Plan getPlan() {
        return plan;
    }

    public String getSummary() {
        return summary;
    }

    public SubArea getSubarea() {
        return subarea;
    }

    public List<String> getLatestObservation() {
        return latestObservations;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public boolean isInConversation() {
        return isInConversation;
    }

    public void setInConversation(boolean inConversation) {
        isInConversation = inConversation;
    }

    public void setCurrentAction(String currentAction) {
        this.currentAction = currentAction;
    }

    @Override
    public String toString() {
        return "Agent{" +
            "name='" + name + '\'' +
            ", age=" + age +
            ", traits=" + traits +
            ", circadianRhythm='" + circadianRhythm + '\'' +
            ", house=" + house.name() +
            ", localMap=" + localMap.areaNames() +
            ", plan=" + plan +
            ", summary='" + summary + '\'' +
            ", subarea=" + subarea.name() +
            ", currentAction='" + currentAction + '\'' +
            ", latestObservations=" + latestObservations +
            ", importanceCounter=" + importanceCounter +
            ", isInConversation=" + isInConversation +
            '}';
    }
}
