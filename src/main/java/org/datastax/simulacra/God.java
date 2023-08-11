package org.datastax.simulacra;

import com.datastax.oss.driver.api.core.CqlSession;
import org.datastax.simulacra.agents.Agent;
import org.datastax.simulacra.agents.AgentRegistry;
import org.datastax.simulacra.conversation.ConversationsRegistry;
import org.datastax.simulacra.db.CqlSessionManager;
import org.datastax.simulacra.environment.WorldMap;

import java.util.*;
import java.util.function.Function;

import static java.lang.Integer.parseInt;
import static org.datastax.simulacra.Utils.*;

public class God {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CqlSession session = CqlSessionManager.getSession();

    private static final List<Runnable> will = Collections.synchronizedList(new ArrayList<>());

    private static final LinkedHashMap<String, Runnable> commands = new LinkedHashMap<>() {{
        put("modify an item", God::modifyItem);
        put("dump agent locations", God::dumpLocations);
        put("dump current conversations", God::dumpConversations);
        put("make db query", God::makeDbQuery);
        put("exit", () -> System.out.println("Universe> Goodbye!"));
    }};

    private static boolean waitingForResponse = false;

    public static boolean poll() {
        if (!waitingForResponse) {
            System.out.println("\nUniverse> What do you want to do?");

            int[] i = { 0 };
            commands.keySet().forEach(command -> {
                System.out.println(i[0]++ + ") " + command + "?");
            });

            System.out.print("God> ");
            waitingForResponse = true;
        }

        if (!scanner.hasNextLine()) {
            return false;
        }
        waitingForResponse = false;

        var commandIndex = scanner.nextLine();
        var command = getNthItem(commands, parseInt(commandIndex));

        if (command == null) {
            System.out.println("Universe> Invalid command index.");
            return false;
        }

        command.getValue().run();
        return command.getKey().equals("exit");
    }

    public static List<Runnable> getWill() {
        var commands = new ArrayList<>(God.will);
        God.will.clear();
        return commands;
    }

    private static void modifyItem() {
        System.out.println("\nUniverse> Choose an area from " + WorldMap.GLOBAL.areaNames());
        System.out.print("God> ");

        var area = getOrSuggest(WorldMap.GLOBAL::findArea, WorldMap.GLOBAL.areaNames());
        if (area == null) {
            return;
        }

        System.out.println("\nUniverse> Choose a subarea from " + area.subareaNames());
        System.out.print("God> ");

        var subarea = getOrSuggest(area::findSubarea, area.subareaNames());
        if (subarea == null) {
            return;
        }

        System.out.println("\nUniverse> Choose an item from " + subarea.itemNames());
        System.out.print("God> ");

        var item = getOrSuggest(subarea::findItem, subarea.itemNames());
        if (item == null) {
            return;
        }

        System.out.println("\nUniverse> What is the new status?");
        System.out.print("God> ");

        var newStatus = scanner.nextLine();
        if (newStatus.equalsIgnoreCase("nvm")) {
            return;
        }

        will.add(() -> {
            item.setStatus(newStatus);
        });
    }

    private static void dumpLocations() {
        System.out.println("\nUniverse> Dumping agent locations...");

        AgentRegistry.chunkedBySubarea().forEach(agents -> {
            var subarea = agents.get(0).getSubarea();
            System.out.println(" - " + subarea.area().name() + " > " + subarea.name() + ": " + map(agents, Agent::getName));
        });
    }

    private static void dumpConversations() {
        var conversations = ConversationsRegistry.getSnapshot();

        if (conversations.isEmpty()) {
            System.out.println("\nUniverse> No conversations found.");
            return;
        }

        System.out.println("\nUniverse> Dumping current conversations...");

        conversations.forEach(conversation -> {
            System.out.println(" - " + conversation.getInstigator().getName() + " > " + conversation.getTarget().getName());
        });
    }

    private static void makeDbQuery() {
        System.out.println("\nUniverse> What is the query?");
        System.out.print("God> ");

        var query = scanner.nextLine();
        if (query.equalsIgnoreCase("nvm")) {
            return;
        }

        try {
            var result = session.execute(query);
            System.out.println("\nUniverse> " + result);
        } catch (Exception e) {
            System.out.println("\nUniverse> " + e.getMessage());
        }
    }

    private static <T> T getOrSuggest(Function<String, T> fn, Collection<String> options) {
        var input = scanner.nextLine().toLowerCase();

        if (input.equals("nvm")) {
            return null;
        }

        var attempt = fn.apply(input);

        if (attempt != null) {
            return attempt;
        }

        var closestString = findClosestString(input, options);
        System.out.println("\nUniverse> Did you mean " + closestString + "? (y/N)");
        System.out.print("God> ");

        if (scanner.nextLine().equalsIgnoreCase("y")) {
            return fn.apply(closestString);
        }
        return null;
    }
}
