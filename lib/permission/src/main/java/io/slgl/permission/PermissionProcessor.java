package io.slgl.permission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicBoolean;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.slgl.client.audit.PermissionEvaluationResult;
import io.slgl.client.node.permission.Permission;
import io.slgl.client.utils.jackson.ObjectMapperFactory;
import io.slgl.permission.PermissionEvaluationLogger.CurrentLogEntries;
import io.slgl.permission.context.EvaluationContext;
import io.slgl.permission.utils.JsonLogicNodes;

import java.util.ArrayList;
import java.util.List;

public class PermissionProcessor {

    private final PermissionEvaluationLogger logger;
    private final SlglJsonLogicEvaluator evaluator;
    private final RequirementsProcessor requirementsProcessor;
    private final ObjectMapper mapper = ObjectMapperFactory.createSlglObjectMapper();

    public PermissionProcessor() {
        this.logger = new PermissionEvaluationLogger();
        this.evaluator = new SlglJsonLogicEvaluator(this.logger);
        this.requirementsProcessor = new RequirementsProcessor();
    }

    public PermissionEvaluationResult process(EvaluationContext context, Permission permission) {
        try (CurrentLogEntries entries = logger.startLogging()) {
            boolean evaluationResult = evaluate(context, permission);

            return new PermissionEvaluationResult(
                    permission,
                    context.getAccessedValues(),
                    entries.get(),
                    evaluationResult
            );
        }
    }

    public boolean evaluate(EvaluationContext context, Permission permission) {
        try {
            JsonLogicNode permissionNode = convertToJsonLogicNode(permission);
            Object evaluated = evaluator.evaluate(permissionNode, context);
            return JsonLogic.truthy(evaluated);

        } catch (EvaluationAbortedException e) {
            return false;
        } catch (Throwable e) {
            logger.log("unknown_exception", e.getMessage());
        }
        return false;
    }

    private JsonLogicNode convertToJsonLogicNode(Permission permission) throws JsonLogicException, JsonProcessingException {
        List<JsonLogicNode> result = new ArrayList<>(requirementsProcessor.convertRequirementsToJsonLogic(permission.getRequire()));

        if (permission.getRequireLogic() != null) {
            String requireLogicJson = mapper.writeValueAsString(permission.getRequireLogic());
            result.add(JsonLogicParser.parse(requireLogicJson));
        }

        if (result.isEmpty()) {
            return new JsonLogicBoolean(true);
        }

        return JsonLogicNodes.joinWithAnd(result);
    }
}
