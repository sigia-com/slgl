package io.slgl.api.testdocuments;

import io.slgl.api.domain.UploadedFile;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.template.Template;
import io.slgl.template.TemplateMatcher;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class TestDocumentsReader {

    public static TemplateMatcher createTemplateFromText(String text) {
        return new TemplateMatcher(new Template(text));
    }

    public static TemplateMatcher readTemplate(String resourcePath) throws IOException {
        return new TemplateMatcher(readTemplateEntity(resourcePath));
    }

    public static Template readTemplateEntity(String resourcePath) throws IOException {
        String templateText = IOUtils.toString(readResource(resourcePath));
        return new Template(templateText);
    }

    public static NodeRequest readRequestObject(String resourcePath) throws IOException {
        String json = IOUtils.toString(readResource(resourcePath));
        return NodeRequest.fromJson(json);
    }

    public static UploadedFile readDocumentWithRequest(String resourcePath, NodeRequest nodeRequest) throws IOException {
        byte[] documentBytes = IOUtils.toByteArray(readResource(resourcePath));

        return new UploadedFile(documentBytes) {
            @Override
            public NodeRequest getRequestObject() {
                return nodeRequest != null ? nodeRequest : super.getRequestObject();
            }
        };
    }

    public static InputStream readResource(String path) {
        return TestDocumentsReader.class.getResourceAsStream(path);
    }
}
