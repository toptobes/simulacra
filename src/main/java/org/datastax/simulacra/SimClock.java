package org.datastax.simulacra;

import org.datastax.simulacra.agents.AgentRegistry;
import org.datastax.simulacra.logging.HomemadeLogger;
import org.datastax.simulacra.moment.Moment;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SimClock {
    public static final int TIME_GRANULARITY = 10;
    public static final int TIME_PER_LOOP = 10000;

    public static final Instant RUN_START_TIME = Instant.now();

    private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicReference<Exception> throwableFromThread = new AtomicReference<>();

    public static final LocalDateTime START_TIME = LocalDateTime.of(2023, 1, 13, 7, 0, 0, 0);
    private static LocalDateTime simTime = START_TIME;

    public static void start() {
        exec.scheduleAtFixedRate(() -> {
            try {
                var moment = Moment.forTime();

                AgentRegistry
                    .chunkedBySubarea()
                    .parallelStream()
                    .forEach(moment::elapse);

                if (moment.elapsesTime()) {
                    simTime = simTime.plusMinutes(TIME_GRANULARITY);
                }

                carryOutGodsWill();
                HomemadeLogger.flush();
            } catch (Exception e) {
                throwableFromThread.set(e);
            }
        }, 0, TIME_PER_LOOP, MILLISECONDS);
    }

    public static void checkForError() throws Throwable {
        if (throwableFromThread.get() != null) {
            var throwable = throwableFromThread.getAndSet(null);

            throw (throwable.getCause() != null)
                ? throwable.getCause()
                : throwable;
        }
    }

    public static void shutdown() {
        exec.shutdown();
    }

    private static void carryOutGodsWill() {
        God.getWill().forEach(Runnable::run);
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE MMMM d");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE MMMM d HH:mm");

    public static LocalDateTime time() {
        return simTime;
    }

    public static Duration elapsed() {
        return Duration.between(START_TIME, simTime);
    }

    public static String timeString() {
        return simTime.format(TIME_FORMATTER);
    }

    public static String dateString() {
        return simTime.format(DATE_FORMATTER);
    }

    public static String dateString(int dayOffset) {
        return simTime.plusDays(dayOffset).format(DATE_FORMATTER);
    }

    public static String dateTimeString() {
        return simTime.format(DATE_TIME_FORMATTER);
    }
}
