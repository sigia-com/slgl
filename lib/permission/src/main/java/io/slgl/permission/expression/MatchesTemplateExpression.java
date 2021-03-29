package io.slgl.permission.expression;

import io.github.jamsesso.jsonlogic.ast.JsonLogicArray;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.slgl.client.utils.jackson.ObjectMapperFactory;
import io.slgl.permission.context.EvaluationContext;
import io.slgl.template.Template;
import io.slgl.template.TemplateMatcher;

import java.util.Map;
import java.util.Optional;

public class MatchesTemplateExpression implements JsonLogicExpression {

    @Override
    public String key() {
        return "matches_template";
    }

    @Override
    public Object evaluate(JsonLogicEvaluator evaluator, JsonLogicArray arguments, Object data) throws JsonLogicEvaluationException {
        if (arguments.size() != 2) {
            throw new JsonLogicEvaluationException(key() + " expressions expect exactly 2 arguments");
        }

        Optional<EvaluationContext> documentMap = getDocumentContext(evaluator.evaluate(arguments.get(0), data));
        if (!documentMap.isPresent()) return false;

        Optional<Template> template = getTemplate(evaluator.evaluate(arguments.get(1), data));
        if (!template.isPresent()) return false;

        Optional<String> document = documentMap.flatMap(this::getDocumentText);
        if (!document.isPresent()) return false;

        Optional<Map<String, ?>> requestObject = documentMap.flatMap(this::getRequestObject);
        if (!requestObject.isPresent()) return false;

        TemplateMatcher matcher = new TemplateMatcher(template.get());
        return matcher.isMatching(document.get(), requestObject.get());
    }

    public Optional<Template> getTemplate(Object templateObj) {
        if (templateObj == null) {
            return Optional.empty();
        }
        try {
            Template template = ObjectMapperFactory.createSlglObjectMapper().convertValue(templateObj, Template.class);
            return Optional.of(template);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<EvaluationContext> getDocumentContext(Object documentObj) {
        if (documentObj instanceof EvaluationContext) {
            return Optional.of((EvaluationContext) documentObj);
        }
        if (documentObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> asMap = (Map<String, ?>) documentObj;
            return Optional.of(EvaluationContext.of(asMap));
        }
        return Optional.empty();
    }

    private Optional<Map<String, ?>> getRequestObject(EvaluationContext documentMap) {
        Object requestObject = documentMap.get("request_object");
        if (requestObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> result = (Map<String, ?>) requestObject;
            return Optional.of(result);
        }
        if (requestObject instanceof EvaluationContext) {
            Map<String, Object> result = ((EvaluationContext) requestObject).asInitializedMap();
            return Optional.of(result);
        }
        return Optional.empty();
    }

    public Optional<String> getDocumentText(EvaluationContext documentMap) {
        Object document = documentMap.get("text");
        return document instanceof String ? Optional.of((String) document) : Optional.empty();
    }
}
