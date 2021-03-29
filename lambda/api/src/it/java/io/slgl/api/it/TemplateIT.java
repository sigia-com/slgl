package io.slgl.api.it;

import com.google.common.collect.ImmutableMap;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PdfMother.createPdf;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.TemplateNodeRequest.template;
import static io.slgl.client.node.permission.Allow.allowLink;

public class TemplateIT extends AbstractApiTest {

    @Test
    public void shouldLinkTemplateToType() {
        // given
        NodeResponse type = createType("type");

        WriteRequest request = WriteRequest.builder()
                .addRequest(TemplateNodeRequest.builder()
                        .text("Example agreement."))
                .addLinkRequest(0, type, "#templates")
                .build();

        // when
        WriteResponse response = ledger.write(request);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldValidateTemplateMatchForStandaloneTypeWithStandaloneTemplate() {
        // given
        String templateText = "Example agreement.";

        NodeResponse documentType = createType("type");
        addTemplatesToType(documentType, templateText);

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(templateText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf("Other agreement."))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldValidateTemplateMatchForStandaloneTypeWithInlineTemplate() {
        // given
        String templateText = "Example agreement.";

        NodeResponse documentType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .linkTemplate(TemplateNodeRequest.builder()
                        .text(templateText)));

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(templateText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf("Other agreement."))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldValidateTemplateMatchForInlineTypeWithInlineTemplate() {
        // given
        String templateText = "Example agreement.";

        TypeNodeRequest inlineType = TypeNodeRequest.builder()
                .linkTemplate(TemplateNodeRequest.builder()
                        .text(templateText))
                .build();

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(inlineType)
                .file(createPdf(templateText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(inlineType)
                .file(createPdf("Other agreement."))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldValidateTemplateMatchWhenThereAreMultipleStandaloneAndInlineTemplates() {
        // given
        String inlineTemplateText = "Text of inline template.";
        String standaloneTemplateText = "Text of standalone template.";

        NodeResponse documentType = createTypeWithInlineTemplates("document_type",
                "Other inline template", inlineTemplateText, "Yet another inline template");
        addTemplatesToType(documentType,
                "Other standalone template", standaloneTemplateText, "Yet another standalone template");

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(inlineTemplateText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();

        // when
        response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(standaloneTemplateText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf("Other agreement."))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCreateNodeWithDocumentMatchingTemplateWithUnicodeCharacters() {
        // given
        String templateText = "Example agreement ąęśćłäöü ĄĘŚĆŁÄÖÜ.";

        NodeResponse documentType = createTypeWithTemplate(templateText);

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(templateText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();
    }

    @Test
    public void shouldCreateNodeWithDocumentMatchingTemplateFromParentType() {
        // given
        String templateText = "Example agreement.";

        NodeResponse parentType = createTypeWithTemplate(templateText);

        NodeResponse childType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(parentType.getId()));

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(childType)
                .file(createPdf(templateText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();
    }

    @Test
    public void shouldCreateNodeWithDocumentMatchingTemplateWithPlaceholders() {
        // given
        String templateText = "Example agreement. Description: ${description}. Payment: ${payment.amount} ${payment.currency}.";
        NodeResponse documentType = createTypeWithTemplateAndStateProperties(templateText, "description", "payment");

        Map<String, Object> requestObject = ImmutableMap.of(
                "description", "example description",
                "payment", ImmutableMap.of(
                        "amount", 19,
                        "currency", "EUR"));
        String documentText = templateText
                .replace("${description}", "example description")
                .replace("${payment.amount}", "19")
                .replace("${payment.currency}", "EUR");

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(documentText, requestObject)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();
        assertThat(response.getState())
                .containsEntry("description", requestObject.get("description"))
                .containsEntry("payment", requestObject.get("payment"));
    }

    @Test
    public void shouldCreateNodeWithDocumentMatchingTemplateWithFreemarkerTags() {
        // given
        String templateText = "Example agreement. " +
                "[#if title??]Title: ${title}. [/#if]" +
                "[#if description??]Description: ${description}. [/#if]" +
                "[#if payment??]Payment: ${payment.amount} ${payment.currency}.[/#if]";
        NodeResponse documentType = createTypeWithTemplateAndStateProperties(templateText, "title", "description", "payment");

        Map<String, Object> requestObject = ImmutableMap.of(
                "title", "example title");
        String documentText = "Example agreement. Title: example title.";

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(documentText, requestObject)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();
        assertThat(response.getState())
                .containsEntry("title", requestObject.get("title"))
                .doesNotContainKeys("description", "payment");
    }

    @Test
    public void shouldFailWhenCreatingTemplateWithInvalidFreemarkerTags() {
        // given
        String templateText = "Example agreement. [#if value??]${value}[/no_closing_if]";

        NodeRequest templateRequest = TemplateNodeRequest.builder()
                .text(templateText)
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(templateRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].text", "valid_freemarker_template")
                .fieldErrorContainsMessage("requests[0].text", "You have an unclosed #if.");
    }

    @Test
    public void shouldFailWhenCreatingTypeWithInlineTemplateWithInvalidFreemarkerTags() {
        // given
        String templateText = "Example agreement. [#if value??]${value}[/no_closing_if]";

        NodeRequest request = TypeNodeRequest.builder()
                .linkTemplate(TemplateNodeRequest.builder()
                        .text(templateText))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].#templates[0].text", "valid_freemarker_template")
                .fieldErrorContainsMessage("requests[0].#templates[0].text", "You have an unclosed #if.");
    }

    @Test
    public void shouldFailWhenCreatingNestedInlineTypeWithInlineTemplateWithInvalidFreemarkerTags() {
        // given
        String templateText = "Example agreement. [#if value??]${value}[/no_closing_if]";

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#documents", TypeNodeRequest.builder()
                                .linkTemplate(TemplateNodeRequest.builder()
                                        .text(templateText))))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].@type.anchors[0].type.#templates[0].text", "valid_freemarker_template")
                .fieldErrorContainsMessage("requests[0].@type.anchors[0].type.#templates[0].text", "You have an unclosed #if.");
    }

    @Test
    public void shouldCreateNodeWithDocumentMatchingTemplateWithBulletList() {
        // given
        String templateText = "Bullet list:\n  * apples\n  * oranges\n  * pears\n\n";

        NodeResponse documentType = createTypeWithTemplate(templateText);

        String documentText = "Bullet list: - apples - oranges - pears ";

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(documentText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();
    }

    @Test
    public void shouldCreateNodeWithDocumentMatchingTemplateWithCustomMarkdownListMarkerPatterns() {
        // given
        String templateText = "Bullet list:\n  * apples\n  * oranges\n  * pears\n\n";

        NodeResponse documentType = ledger.writeNode(TypeNodeRequest.builder()
                .linkNode("#templates", NodeRequest.builder()
                        .data("text", templateText)
                        .data("unordered_list_marker_pattern", "#")));

        String documentText = "Bullet list: # apples # oranges # pears ";

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(documentText)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();
    }

    @Test
    public void shouldFailWhenCreatingNodeWithDocumentNotMatchingCustomMarkdownListMarkerPatterns() {
        // given
        String templateText = "Bullet list:\n  * apples\n  * oranges\n  * pears\n\n";

        NodeResponse documentType = ledger.writeNode(TypeNodeRequest.builder()
                .linkNode("#templates", NodeRequest.builder()
                        .data("text", templateText)
                        .data("unordered_list_marker_pattern", "#")));

        String documentText = "Bullet list: - apples - oranges - pears ";

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(documentText))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldFailWhenCreatingNodeWithDocumentWithoutValuesForPlaceholders() {
        // given
        String templateText = "Example agreement. Placeholder: ${placeholder}.";
        NodeResponse documentType = createTypeWithTemplate(templateText);

        Map<String, Object> requestObject = ImmutableMap.of();
        String documentText = "Example agreement. Placeholder: x.";

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(documentText, requestObject))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldFailWhenCreatingNodeWithDocumentWithInvalidValuesForPlaceholders() {
        // given
        String templateText = "Example agreement. Placeholder: ${placeholder}.";
        NodeResponse documentType = createTypeWithTemplate(templateText);

        Map<String, Object> requestObject = ImmutableMap.of(
                "placeholder", ImmutableMap.of(
                        "inner-key", "inner-value"));
        String documentText = "Example agreement. Placeholder: x.";

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf(documentText, requestObject))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldIgnoreRequestObjectFromDocumentWhenTypeDoesNotHaveTemplate() {
        // given
        NodeResponse documentType = createTypeWithStateProperties("description", "payment");

        Map<String, Object> requestObject = ImmutableMap.of(
                "description", "example description",
                "payment", ImmutableMap.of(
                        "amount", 19,
                        "currency", "EUR"));

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .type(documentType)
                .file(createPdf("Example document text.", requestObject)));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileSha3()).isNotEmpty();
        assertThat(response.getState()).doesNotContainKeys("description", "payment");
    }

    private NodeResponse createTypeWithInlineTemplates(String type, String... templateTexts) {
        TypeNodeRequest.Builder request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .extendsType(Types.TYPE)
                        .permission(Permission.builder()
                                .allow(allowLink("#templates"))
                                .alwaysAllowed()));

        for (String templateText : templateTexts) {
            request.linkTemplate(template(templateText));
        }

        return ledger.writeNode(request);
    }

    private NodeResponse createTypeWithTemplate(String templateText) {
        return createTypeWithTemplates(templateText);
    }

    private NodeResponse createTypeWithTemplateAndStateProperties(String templateText, String... stateProperties) {
        return addTemplatesToType(createTypeWithStateProperties(stateProperties), templateText);
    }

    private NodeResponse createTypeWithStateProperties(String... stateProperties) {
        return ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .extendsType(Types.TYPE)
                        .permission(Permission.builder()
                                .allow(allowLink("#templates"))
                                .alwaysAllowed()))
                .stateProperties(stateProperties));
    }

    private NodeResponse createTypeWithTemplates(String... templateTexts) {
        return addTemplatesToType(createType("document_type"), templateTexts);
    }

    private NodeResponse addTemplatesToType(NodeResponse documentType, String... templateTexts) {
        for (String templateText : templateTexts) {
            ledger.write(WriteRequest.builder()
                    .addRequest(TemplateNodeRequest.builder()
                            .text(templateText))
                    .addLinkRequest(0, documentType, "#templates"));
        }

        return documentType;
    }

    protected NodeResponse createType(String name) {
        TypeNodeRequest request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .extendsType(Types.TYPE)
                        .permission(Permission.builder()
                                .allow(allowLink("#templates"))
                                .alwaysAllowed()))
                .build();

        return ledger.writeNode(request);
    }
}
