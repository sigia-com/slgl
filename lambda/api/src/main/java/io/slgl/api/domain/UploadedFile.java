package io.slgl.api.domain;

import com.google.common.annotations.VisibleForTesting;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;
import io.slgl.api.ExecutionContext;
import io.slgl.api.context.principal.DocumentSignaturePrincipal;
import io.slgl.api.document.model.DocumentSignatureInfo;
import io.slgl.api.document.service.DocumentSignatureInfoFactory;
import io.slgl.api.document.service.PdfMetadataContextFactory;
import io.slgl.api.error.ApiException;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.Utils;
import io.slgl.client.utils.PdfUtils;
import io.slgl.permission.context.EvaluationContext;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Data
public class UploadedFile {

    private static final Logger log = LoggerFactory.getLogger(UploadedFile.class);
    private static final Pattern METADATA_STRING_PATTERN = Pattern.compile("^(.+)\\$string$");
    private static final Pattern METADATA_JSON_PATTERN = Pattern.compile("^.+$");

    private final byte[] bytes;

    private final DocumentSignatureInfoFactory documentSignatureInfoFactory = ExecutionContext.get(DocumentSignatureInfoFactory.class);
    private final PdfMetadataContextFactory pdfMetadataContextFactory = ExecutionContext.get(PdfMetadataContextFactory.class);

    private String fileSha3;
    private String contentSha3;

    private PdfDocument document;
    private String documentText;
    private List<DocumentSignatureInfo> documentSignatureInfos;

    private Boolean pdf;

    public UploadedFile(byte[] bytes) {
        this.bytes = bytes;
    }

    public EvaluationContext asEvaluationContext() {
        var result = EvaluationContext.builder()
                .provider("request_object", this::getRequestObject)
                .provider("file_sha3", this::buildFileSha3)
                .provider("is_pdf", this::isPdf);

        if (isPdf()) {
            result.provider("text", () -> getDocumentText().orElse(null))
                    .provider("content_sha3", this::getVisualContentSha)
                    .provider("pdf_info", this::getPdfInfoContext)
                    .provider("document_signatures", this::getDocumentSignatures);
        }

        return result.build();
    }

    @VisibleForTesting
    public EvaluationContext getPdfInfoContext() {
        var pdfInfo = getDocument().getTrailer().get(PdfName.Info);
        var pdfObject = pdfInfo instanceof PdfDictionary
                ? ((PdfDictionary) pdfInfo)
                : new PdfDictionary();
        return pdfMetadataContextFactory.createEvaluationContext(pdfObject);
    }

    public NodeRequest getRequestObject() {
        if (!isPdf()) {
            return new NodeRequest();
        }

        String jsonMetadata = getDocument().getDocumentInfo().getMoreInfo("json_metadata");
        if (isBlank(jsonMetadata)) {
            return new NodeRequest();
        }

        return NodeRequest.fromJson(jsonMetadata);
    }

    public String buildFileSha3() {
        if (fileSha3 == null) {
            fileSha3 = Utils.getSha3OfBytes(bytes);
        }
        return fileSha3;
    }

    @VisibleForTesting
    public List<DocumentSignatureInfo> getDocumentSignatures() {
        if (documentSignatureInfos == null) {
            documentSignatureInfos = documentSignatureInfoFactory.getSignatureInfo(bytes)
                    .stream()
                    .filter(DocumentSignatureInfo::hasNotFailed)
                    .collect(Collectors.toList());
        }
        return documentSignatureInfos;
    }

    private String getVisualContentSha() {
        if (contentSha3 == null) {
            try {
                contentSha3 = PdfUtils.getVisualContentHashHex(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return contentSha3;
    }

    public Optional<String> getDocumentText() {
        if (!isPdf()) {
            return Optional.empty();
        }

        if (documentText == null) {
            StringBuilder result = new StringBuilder();

            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                PdfPage page = document.getPage(i);
                String pageText = PdfTextExtractor.getTextFromPage(page, new LocationTextExtractionStrategy());

                if (result.length() > 0) {
                    result.append("\n\n");
                }
                result.append(pageText);
            }

            documentText = result.toString();
        }

        return Optional.of(documentText);
    }

    private PdfDocument getDocument() {
        if (document == null) {
            try {
                PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(bytes));
                document = new PdfDocument(pdfReader);
            } catch (IOException | com.itextpdf.io.IOException e) {
                throw new ApiException(ErrorCode.LINKED_DOCUMENT_MUST_BE_PDF);
            }
        }

        return document;
    }

    public Boolean isPdf() {
        if (pdf == null) {
            try {
                getDocument();
                pdf = true;
            } catch (ApiException e) {
                if (ErrorCode.LINKED_DOCUMENT_MUST_BE_PDF == e.getErrorCode()) {
                    pdf = false;
                } else {
                    throw e;
                }
            }
        }
        return pdf;
    }

    public List<DocumentSignaturePrincipal> getCertificatePrincipals() {
        return getDocumentSignatures()
                .stream()
                .map(DocumentSignatureInfo::asEvaluationContext)
                .map(EvaluationContext::asInitializedMap)
                .map(DocumentSignaturePrincipal::new)
                .collect(Collectors.toList());
    }
}
