package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.function.BiPredicate;

public class DateTimeComparisonExpression implements PreEvaluatedArgumentsExpression {

    public static final DateTimeComparisonExpression BEFORE = new DateTimeComparisonExpression("before", Instant::isBefore);
    public static final DateTimeComparisonExpression AFTER = new DateTimeComparisonExpression("after", Instant::isAfter);

    private static final DateTimeFormatter ISO_DATE_TIME_IN_UTC = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    private final String key;
    private final BiPredicate<Instant, Instant> predicate;

    private DateTimeComparisonExpression(String key, BiPredicate<Instant, Instant> predicate) {
        this.key = key;
        this.predicate = predicate;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Object evaluate(List arguments, Object data) throws JsonLogicEvaluationException {
        if (arguments.size() != 2) {
            throw new JsonLogicEvaluationException("'" + key + "' requires 2 arguments");
        }

        Instant left = parseDateTime(arguments.get(0));
        Instant right = parseDateTime(arguments.get(1));

        if (left == null || right == null) {
            return false;
        }

        return predicate.test(left, right);
    }

    private Instant parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Temporal) {
            return Instant.from(((Temporal) value));
        }
        return ISO_DATE_TIME_IN_UTC.parse(value.toString(), Instant::from);
    }
}
