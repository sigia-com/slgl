package io.slgl.api.context.principal;

import com.google.common.collect.ImmutableMap;
import io.slgl.permission.context.EvaluationContext;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApiPrincipal implements Principal {

    private String username;

    @Override
    public EvaluationContext asEvaluationContext() {
        return EvaluationContext.builder()
                .value("api", ImmutableMap.of("username", username))
                .build();
    }
}
