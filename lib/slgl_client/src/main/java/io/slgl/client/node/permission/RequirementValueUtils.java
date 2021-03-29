package io.slgl.client.node.permission;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.Optional;

public class RequirementValueUtils {
    public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static String dateToString(Temporal temporal) {
        return DEFAULT_TIME_FORMATTER.format(temporal);
    }

    public static Object toSlglValue(Object object) {
        if (object instanceof Temporal) {
            return dateToString(((Temporal) object));
        }
        return object;
    }

    public static boolean areValuesEqual(Object object, Object other) {
        if (object == null || other == null) {
            return object == other;
        }
        if (object instanceof Temporal || other instanceof Temporal) {
            Optional<Instant> aDateTime = toInstant(object);
            Optional<Instant> bDateTime = toInstant(other);
            return aDateTime.isPresent() && bDateTime.isPresent()
                    && Objects.equals(aDateTime, bDateTime);
        }
        return Objects.equals(object, other);
    }

    private static Optional<Instant> toInstant(Object object) {
        if (object instanceof ChronoZonedDateTime<?>) {
            return Optional.of(((ChronoZonedDateTime<?>) object).toInstant());
        }
        if (object instanceof OffsetDateTime) {
            return Optional.of(((OffsetDateTime) object).toInstant());
        }
        if (object instanceof String) {
            Instant instant = DEFAULT_TIME_FORMATTER.parse(((String) object), Instant::from);
            return Optional.of(instant);
        }
        return Optional.empty();
    }
}
