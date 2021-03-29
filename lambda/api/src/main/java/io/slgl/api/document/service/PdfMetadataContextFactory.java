package io.slgl.api.document.service;

import com.itextpdf.kernel.pdf.*;
import io.slgl.permission.context.EvaluationContext;

import java.util.ArrayList;
import java.util.Collections;


public class PdfMetadataContextFactory {

    public Object createEvaluationContext(PdfObject pdfObject) {
        if (pdfObject == null) {
            return null;
        }
        switch (pdfObject.getType()) {

            case PdfObject.ARRAY:
                return createEvaluationContext((PdfArray) pdfObject);

            case PdfObject.BOOLEAN:
                return createEvaluationContext((PdfBoolean) pdfObject);

            case PdfObject.DICTIONARY:
                return createEvaluationContext(((PdfDictionary) pdfObject));

            case PdfObject.INDIRECT_REFERENCE:
                return createEvaluationContext((PdfIndirectReference) pdfObject);

            case PdfObject.NAME:
                return createEvaluationContext((PdfName) pdfObject);

            case PdfObject.NUMBER:
                return createEvaluationContext((PdfNumber) pdfObject);

            case PdfObject.STREAM:
                return createEvaluationContext(((PdfStream) pdfObject));

            case PdfObject.STRING:
                return createEvaluationContext((PdfString) pdfObject);

            case PdfObject.LITERAL:
            case PdfObject.NULL:
                return null;

            default:
                return EvaluationContext.of(Collections.emptyMap());
        }
    }

    private String createEvaluationContext(PdfString pdfObject) {
        return pdfObject.getValue();
    }

    private double createEvaluationContext(PdfNumber pdfObject) {
        return pdfObject.getValue();
    }

    private String createEvaluationContext(PdfName pdfObject) {
        return pdfObject.getValue();
    }

    private Object createEvaluationContext(PdfIndirectReference pdfObject) {
        var refersTo = pdfObject.getRefersTo();
        return createEvaluationContext(refersTo);
    }

    private boolean createEvaluationContext(PdfBoolean pdfObject) {
        return pdfObject.getValue();
    }

    public EvaluationContext createEvaluationContext(PdfDictionary dictionary) {
        var result = EvaluationContext.builder();
        for (PdfName name : dictionary.keySet()) {
            result.provider(name.getValue(), () -> createEvaluationContext(dictionary.get(name)));
        }
        return result.build();
    }

    private ArrayList<Object> createEvaluationContext(PdfArray pdfArray) {
        var result = new ArrayList<>();
        for (PdfObject pdfObject : pdfArray) {
            result.add(createEvaluationContext(pdfObject));
        }
        return result;
    }

}
