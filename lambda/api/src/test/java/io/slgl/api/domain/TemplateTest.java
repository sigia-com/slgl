package io.slgl.api.domain;

import com.google.common.collect.ImmutableMap;
import io.slgl.api.ExecutionContext;
import io.slgl.api.document.service.TrustListManagementService;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.template.TemplateMatcher;
import io.slgl.template.TemplateMatcher.NoAllKeysAccessedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static io.slgl.api.testdocuments.TestDocumentsReader.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

public class TemplateTest {

    @BeforeEach
    void setUp() {
        ExecutionContext.reset();

        ExecutionContext.put(S3Client.class, null);
        ExecutionContext.put(TrustListManagementService.class, TrustListManagementService.offline());
    }

    @AfterEach
    void cleanup() {
        ExecutionContext.reset();
    }

    @Test
    public void shouldMatchRealWorldDocumentToTemplate() throws Exception {
        // given
        TemplateMatcher template = readTemplate("example-document.tpl");

        NodeRequest nodeRequest = readRequestObject("example-document.json");
        UploadedFile document = readDocumentWithRequest("example-document.pdf", nodeRequest);

        // when
        boolean isMatching = template.isMatching(document.getDocumentText().get(), nodeRequest.getData());

        // then
        assertThat(isMatching).isTrue();
    }

    @Test
    public void shouldValidateAccessToAllKeys() throws Exception {
        // given
        TemplateMatcher template = createTemplateFromText("test = ${foo}\ntest2 = ${bar.baz}");

        NodeRequest nodeRequest = new NodeRequest();
        nodeRequest.setData(ImmutableMap.of(
                "foo", 123,
                "bar", ImmutableMap.of("baz", "123")
        ));

        // then
        assertThatCode(() -> template.renderTemplate(nodeRequest.getData()))
                .doesNotThrowAnyException();
    }

    @Test
    public void shouldThrowExceptionWhenNoAllKeysWereAccessed() throws Exception {
        // given
        TemplateMatcher template = createTemplateFromText("no access to context");

        NodeRequest nodeRequest = new NodeRequest();
        nodeRequest.setData(ImmutableMap.of(
                "foo", 123,
                "bar", ImmutableMap.of("baz", "123")
        ));

        // then
        assertThatThrownBy(() -> template.renderTemplate(nodeRequest.getData()))
                .asInstanceOf(type(NoAllKeysAccessedException.class))
                .extracting(NoAllKeysAccessedException::getNotAccessedKeys, iterable(String.class))
                .containsOnly("foo", "bar.baz");
    }
}