package io.slgl.api.document.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.document.model.DocumentSignatureInfo;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentSignatureInfoFactoryTest {

    private DocumentSignatureInfoFactory signatureInfoFactory;

    @BeforeEach
    void setup() {
        ExecutionContext.reset();
        ExecutionContext.put(S3Client.class, null);
        ExecutionContext.put(TrustListManagementService.class, TrustListManagementService.offline());

        signatureInfoFactory = ExecutionContext.get(DocumentSignatureInfoFactory.class);
    }

    @AfterEach
    void cleanup() {
        ExecutionContext.reset();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/io/slgl/api/pdfshadowattack/hide/variant-1_hide-via-referenced-object/4_original-document-shadowed-signed-manipulated_v1.pdf",
            "/io/slgl/api/pdfshadowattack/hide/variant-1_hide-via-referenced-object/4_original-document-shadowed-signed-manipulated_v2.pdf",
            "/io/slgl/api/pdfshadowattack/hide/variant-2_hide-via-referenced-object/hide-form-via-form/4_original-document-shadowed-signed-manipulated.pdf",
            "/io/slgl/api/pdfshadowattack/hide/variant-2_hide-via-referenced-object/hide-text-via-form/4_original-document-shadowed-signed-manipulated.pdf",
            "/io/slgl/api/pdfshadowattack/replace/variant-1_replace-via-overlay/4_original-document-shadowed-signed-manipulated.pdf",
            "/io/slgl/api/pdfshadowattack/replace/variant-2_replace-via-overwrite/4_original-document-shadowed-signed-manipulated.pdf",
            "/io/slgl/api/pdfshadowattack/hide-and-replace/variant-1_change_object_references/4_original-document-shadowed-signed-manipulated.pdf",
            "/io/slgl/api/pdfshadowattack/hide-and-replace/variant-2_change_objects_usage/4_original-document-shadowed-signed-manipulated.pdf",
    })
    void shouldBeInvalidAfterShadowAttack(String filename) throws IOException {
        var pdfBytes = IOUtils.resourceToByteArray(filename);
        var signatures = signatureInfoFactory.getSignatureInfo(pdfBytes);
        assertThat(signatures)
                .noneMatch(DocumentSignatureInfo::doesCoverWholeDocument);
        // TODO should be reevaluated in the future
        // .noneMatch(DocumentSignatureInfo::hasNotFailed);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/io/slgl/api/pdfshadowattack/hide/variant-1_hide-via-referenced-object/3_original-document-shadowed-signed.pdf",
            "/io/slgl/api/pdfshadowattack/hide/variant-2_hide-via-referenced-object/hide-form-via-form/3_original-document-shadowed-signed.pdf",
            "/io/slgl/api/pdfshadowattack/hide/variant-2_hide-via-referenced-object/hide-text-via-form/3_original-document-shadowed-signed.pdf",
            "/io/slgl/api/pdfshadowattack/replace/variant-1_replace-via-overlay/3_original-document-shadowed-signed.pdf",
            "/io/slgl/api/pdfshadowattack/replace/variant-2_replace-via-overwrite/3_original-document-shadowed-signed.pdf",
            "/io/slgl/api/pdfshadowattack/hide-and-replace/variant-1_change_object_references/3_original-document-shadowed-signed.pdf",
            "/io/slgl/api/pdfshadowattack/hide-and-replace/variant-2_change_objects_usage/3_original-document-shadowed-signed.pdf",
    })
    void shouldBeValidBeforeShadowAttack(String filename) throws IOException {
        var pdfBytes = IOUtils.resourceToByteArray(filename);
        var signatures = signatureInfoFactory.getSignatureInfo(pdfBytes);
        assertThat(signatures)
                .allMatch(DocumentSignatureInfo::doesCoverWholeDocument)
                .allMatch(DocumentSignatureInfo::hasNotFailed);
    }
}