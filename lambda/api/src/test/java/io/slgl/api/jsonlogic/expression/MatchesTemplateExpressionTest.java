package io.slgl.api.jsonlogic.expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.slgl.api.ExecutionContext;
import io.slgl.api.document.service.TrustListManagementService;
import io.slgl.api.domain.UploadedFile;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.testdocuments.TestDocumentsReader;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.audit.PermissionEvaluationResult;
import io.slgl.client.node.permission.Permission;
import io.slgl.client.node.permission.Requirement;
import io.slgl.client.node.permission.Requirements;
import io.slgl.permission.PermissionProcessor;
import io.slgl.permission.context.EvaluationContext;
import io.slgl.template.Template;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class MatchesTemplateExpressionTest {

    private PermissionProcessor permissionProcessor;

    @BeforeEach
    public void setup() {
        ExecutionContext.reset();

        ExecutionContext.put(S3Client.class, null);
        ExecutionContext.put(TrustListManagementService.class, TrustListManagementService.offline());

        permissionProcessor = new PermissionProcessor();
    }

    @AfterEach
    void cleanup() {
        ExecutionContext.reset();
    }

    @Test
    public void shouldMatch() throws Exception {
        // given
        Template template = TestDocumentsReader.readTemplateEntity("example-document.tpl");
        NodeRequest nodeRequest = TestDocumentsReader.readRequestObject("example-document.json");
        UploadedFile document = TestDocumentsReader.readDocumentWithRequest("example-document.pdf", nodeRequest);

        // when
        PermissionEvaluationResult result = evaluate(template, document);

        // then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void shouldNotMatch() throws Exception {
        // given
        Template template = new Template("not matching template!");
        NodeRequest nodeRequest = TestDocumentsReader.readRequestObject("example-document.json");
        UploadedFile document = TestDocumentsReader.readDocumentWithRequest("example-document.pdf", nodeRequest);

        // when
        PermissionEvaluationResult result = evaluate(template, document);

        // then
        assertThat(result.isSuccess()).isFalse();
    }

    private PermissionEvaluationResult evaluate(Template template, UploadedFile document) {
        return permissionProcessor.process(
                EvaluationContext.of(ImmutableMap.of(
                        "$file", document.asEvaluationContext(),
                        "$test_template_variable", UncheckedObjectMapper.MAPPER.convertValue(template, JsonNode.class)
                )),
                Permission.builder()
                        .require(Requirements.single(Requirement.requireThat("$file").matchesTemplate().ref("$test_template_variable")))
                        .build()
        );
    }
}