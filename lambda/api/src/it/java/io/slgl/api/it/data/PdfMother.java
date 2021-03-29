package io.slgl.api.it.data;

import io.slgl.api.utils.json.UncheckedObjectMapper;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public class PdfMother {

    private PdfMother() {
    }

    public static byte[] createPdf(String text) {
        return createPdf(text, null, null);
    }

    public static byte[] createPdf(String text, Object jsonObject) {
        return createPdf(text, jsonObject, null);
    }

    public static byte[] createPdf(String text, Object jsonObject, Map<String, ?> additionalMetadata) {
        try {
            PDDocument document = new PDDocument();
            if (jsonObject != null) {
                String jsonMetadata = jsonObject instanceof String
                        ? ((String) jsonObject)
                        : UncheckedObjectMapper.MAPPER.writeValueAsString(jsonObject);
                setOnCosObject(
                        document.getDocumentInformation().getCOSObject(),
                        Map.of("json_metadata", jsonMetadata)
                );
            }
            if (additionalMetadata != null) {
                setOnCosObject(
                        document.getDocumentInformation().getCOSObject(),
                        additionalMetadata
                );
            }

            PDType0Font font = loadFont(document);

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
            contentStream.setFont(font, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, 700);
            for (String line : text.split("\\n")) {
                contentStream.showText(line);
                contentStream.newLine();
                contentStream.newLineAtOffset(0, -20);
            }
            contentStream.endText();
            contentStream.close();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            document.save(stream);

            document.close();

            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static COSBase getCosObject(Object value) {
        if (value == null) {
            return COSNull.NULL;
        }
        if (value instanceof Map) {
            //noinspection unchecked
            return setOnCosObject(new COSDictionary(), (Map<String, ?>) value);
        }
        if (value instanceof Collection) {
            var result = new COSArray();
            for (Object item : ((Collection<?>) value)) {
                var cosItem = getCosObject(item);
                result.add(cosItem);
            }
            return result;
        }
        if (value instanceof String) {
            return new COSString((String) value);
        }
        if (value instanceof Integer) {
            return COSInteger.get(((Integer) value));
        }
        if (value instanceof Long) {
            return COSInteger.get((Long) value);
        }
        if (value instanceof Boolean) {
            return COSBoolean.getBoolean((Boolean) value);
        }
        if (value instanceof Float) {
            return new COSFloat((Float) value);
        }
        if (value instanceof Double) {
            try {
                return new COSFloat(value.toString());
            } catch (IOException e) {
                throw new RuntimeException("should not happen", e);
            }
        }

        throw new IllegalArgumentException("Cannot create COS pdf object for: " + value.getClass());
    }

    private static COSDictionary setOnCosObject(COSDictionary target, Map<String, ?> values) {
        values.forEach((name, value) -> {
            if (target.getItem(name) != null) {
                throw new IllegalArgumentException("Cannot redefine name: `" + name + '`');
            }
            target.setItem(name, getCosObject(value));
        });
        return target;
    }

    private static PDType0Font loadFont(PDDocument document) throws IOException {
        ClassLoader classLoader = PdfMother.class.getClassLoader();

        try (InputStream fontStream = classLoader.getResourceAsStream("fonts/Ubuntu-Medium.ttf")) {
            return PDType0Font.load(document, fontStream);
        }
    }

    public static byte[] createSignedPdf(String text) {
        return createSignedPdf(text, null);
    }

    public static byte[] createSignedPdf(String text, Object object) {
        return createSignedPdf(text, object, null);
    }

    public static byte[] createSignedPdf(String text, Object object, Map<String, ?> metadata) {
        return PdfSignUtils.signPdf(createPdf(text, object, metadata));
    }
}
