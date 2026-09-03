package it.brunasti.mitire.ui.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Formatters {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private Formatters() {
    }

    public static String timestamp(Instant instant) {
        return instant == null ? "" : TIMESTAMP.format(instant);
    }
}
