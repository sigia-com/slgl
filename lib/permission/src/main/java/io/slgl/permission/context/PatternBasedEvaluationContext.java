package io.slgl.permission.context;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PatternBasedEvaluationContext extends AbstractEvaluationContext {

    private final Pattern pattern;
    private final BiFunction<Matcher, String, ?> provide;

    PatternBasedEvaluationContext(Pattern pattern, BiFunction<Matcher, String, ?> provide) {
        this.pattern = pattern;
        this.provide = provide;
    }

    @Override
    protected Object retrieve(String key) {
        Matcher matcher = pattern.matcher(key);
        if (matcher.matches()) {
            return provide.apply(matcher, key);
        }
        return null;
    }

    @Override
    public Map<String, Object> asInitializedMap() {
        throw new UnsupportedOperationException("asInitializedMap() is not supported by " + PatternBasedEvaluationContext.class.getSimpleName());
    }
}
