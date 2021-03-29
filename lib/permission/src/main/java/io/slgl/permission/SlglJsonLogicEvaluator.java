package io.slgl.permission;

import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.*;
import io.slgl.permission.expression.ArrayHasExpression;
import io.slgl.permission.expression.MapExpression;
import io.slgl.permission.expression.ReduceExpression;
import io.slgl.permission.expression.*;

import java.util.Arrays;
import java.util.List;

public class SlglJsonLogicEvaluator extends JsonLogicEvaluator {

    private final JsonLogicVariableEvaluator variableEvaluator;

    public SlglJsonLogicEvaluator(PermissionEvaluationLogger logger) {
        super(getDefaultExpressions(logger));
        this.variableEvaluator = new JsonLogicVariableEvaluator(false, logger);
    }

    @Override
    public Object evaluate(JsonLogicVariable variable, Object data) throws JsonLogicEvaluationException {
        if (data != null) {
            Object key = evaluate(variable.getKey(), data);

            if (key instanceof String) {
                String path = (String) key;
                Object defaultValue = evaluate(variable.getDefaultValue(), null);

                return variableEvaluator.evaluateVariable(path, data, defaultValue);
            }
        }

        return super.evaluate(variable, data);
    }

    private static List<JsonLogicExpression> getDefaultExpressions(PermissionEvaluationLogger logger) {
        return Arrays.asList(

                MathExpression.ADD,
                MathExpression.SUBTRACT,
                MathExpression.MULTIPLY,
                MathExpression.DIVIDE,
                MathExpression.MODULO,
                MathExpression.MIN,
                MathExpression.MAX,
                NumericComparisonExpression.GT,
                NumericComparisonExpression.GTE,
                NumericComparisonExpression.LT,
                NumericComparisonExpression.LTE,
                IfExpression.IF,
                IfExpression.TERNARY,
                EqualityExpression.INSTANCE,
                InequalityExpression.INSTANCE,
                StrictEqualityExpression.INSTANCE,
                StrictInequalityExpression.INSTANCE,
                NotExpression.SINGLE,
                NotExpression.DOUBLE,
                LogicExpression.AND,
                LogicExpression.OR,
                LogExpression.STDOUT,
                AllExpression.INSTANCE,
                MergeExpression.INSTANCE,
                InExpression.INSTANCE,
                ConcatenateExpression.INSTANCE,
                SubstringExpression.INSTANCE,

                MissingExpression.ALL,
                MissingExpression.SOME,

                new ConstantExpression(),
                new VarMapExpression(logger),
                new StaticNodeExpression(logger),

                DateTimeComparisonExpression.BEFORE,
                DateTimeComparisonExpression.AFTER,

                ContainsExpression.CONTAINS,
                ContainsExpression.DOES_NOT_CONTAIN,
                new ContainsAnyOfExpression(),

                new MatchesTemplateExpression(),

                new ArraySomeExpression(logger),
                ArrayHasExpression.NONE,
                ArrayHasExpression.ALL,

                new MapExpression(),
                new io.slgl.permission.expression.FilterExpression(),
                new ReduceExpression()
        );
    }

}
