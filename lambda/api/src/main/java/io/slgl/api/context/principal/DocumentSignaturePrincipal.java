package io.slgl.api.context.principal;

import io.slgl.permission.context.EvaluationContext;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class DocumentSignaturePrincipal implements Principal {

    private Map<String, Object> signatureData;

    public DocumentSignaturePrincipal(Map<String, Object> signatureData) {
        this.signatureData = signatureData;
    }

    @Override
    public EvaluationContext asEvaluationContext() {
        return EvaluationContext
                .builder()
                .value("signature", signatureData)
                .build();
    }
}
