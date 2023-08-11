package org.datastax.simulacra.memorystream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.datastax.simulacra.logging.HomemadeLogger.log;

public record Plan(
    List<String> planForTheDay,
    List<String> planForTheHour
) {
    public Plan(String planForTheDay) {
        this(parseDailyPlan(planForTheDay), new ArrayList<>());
    }

    public Plan(List<String> planForTheDay, String planForTheHour) {
        this(planForTheDay, parseHourlyPlan(planForTheHour));
    }

    private static final Pattern pattern = Pattern.compile("^(\\d{1,2})");

    private static List<String> parseDailyPlan(String plan) {
        var split = plan.split("\n");

        var firstHour = extractHour(split[0]);
        var lastHour = extractHour(split[split.length - 1]);

        var list = new ArrayList<String>(24);

        for (int i = 0; i < firstHour; i++) {
            list.add(i + ":00) Sleep");
        }

        list.addAll(Arrays.asList(split));

        for (int i = lastHour + 1; i < 24; i++) {
            list.add(i + ":00) Sleep");
        }

        return list;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static int extractHour(String plan) {
        var matcher = pattern.matcher(plan);
        matcher.find();
        return Integer.parseInt(matcher.group(1));
    }

    private static List<String> parseHourlyPlan(String plan) {
        log(plan);
        var split = plan.split("\n");
        return Arrays.asList(split);
    }
}
