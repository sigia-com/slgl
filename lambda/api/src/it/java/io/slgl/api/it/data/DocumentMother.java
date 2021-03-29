package io.slgl.api.it.data;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class DocumentMother {

    public static String getTemplateText() {
        return "Example document\nName: ${name}\nValue: ${value}";
    }

    public static Map<String, Object> getDocumentData() {
        return ImmutableMap.of(
                "name", "John Doe",
                "value", 42);
    }

    public static String getDocumentText() {
        Map<String, Object> data = getDocumentData();

        return getTemplateText()
                .replace("${name}", String.valueOf(data.get("name")))
                .replace("${value}", String.valueOf(data.get("value")));

    }

    public static byte[] createPdfDocument() {
        return PdfMother.createPdf(getDocumentText(), getDocumentData());
    }
}
