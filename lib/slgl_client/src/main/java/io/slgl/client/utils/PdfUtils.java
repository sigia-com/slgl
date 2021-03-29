package io.slgl.client.utils;

import com.itextpdf.io.source.ByteUtils;
import com.itextpdf.io.source.PdfTokenizer;
import com.itextpdf.io.source.PdfTokenizer.TokenType;
import com.itextpdf.kernel.pdf.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class PdfUtils {

    private PdfUtils() {
    }

    public static String getVisualContentHashHex(byte[] pdf) throws IOException {
        return HexUtils.toHex(getVisualContentHash(pdf));
    }

    public static byte[] getVisualContentHash(byte[] pdf) throws IOException {
        try {
            return getVisualContentHash(pdf, MessageDigest.getInstance("SHA3-512"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getVisualContentHash(byte[] pdf, MessageDigest digest) throws IOException {
        PdfVisualContentDescription contentDescription = getVisualContentDescription(pdf);

        for (PdfFileRange contentRange : contentDescription.getContentRanges()) {
            digest.update(pdf, (int) contentRange.getStart(), (int) contentRange.getLength());
        }

        return digest.digest();
    }

    public static PdfVisualContentDescription getVisualContentDescription(byte[] pdf) throws IOException {
        PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(pdf));
        PdfDocument document = new PdfDocument(pdfReader);

        PdfObject pages = document.getCatalog().getPdfObject().get(PdfName.Pages);

        PdfVisualContentDescriptionBuilder builder = new PdfVisualContentDescriptionBuilder(document);
        builder.process(pages);
        return builder.build();
    }

    private static class PdfVisualContentDescriptionBuilder {

        private static final byte[] END_OBJ = ByteUtils.getIsoBytes("endobj");
        private static final byte[] STREAM = PdfTokenizer.Stream;

        private final PdfDocument document;
        private final PdfTokenizer tokenizer;

        private final Map<PdfObject, PdfFileRange> ranges = new LinkedHashMap<>();

        public PdfVisualContentDescriptionBuilder(PdfDocument document) {
            this.document = document;
            tokenizer = new PdfTokenizer(document.getReader().getSafeFile());
        }

        public PdfVisualContentDescription build() {
            List<PdfFileRange> sortedRanges = new ArrayList<>(this.ranges.values());
            sortedRanges.sort(Comparator.comparing(PdfFileRange::getStart));

            List<PdfFileRange> result = new ArrayList<>();

            for (PdfFileRange range : sortedRanges) {
                if (result.isEmpty()) {
                    result.add(range);
                } else {
                    PdfFileRange previousRange = result.get(result.size() - 1);

                    if (previousRange.getEnd() == range.getStart()) {
                        result.set(result.size() - 1, new PdfFileRange(previousRange.getStart(), range.getEnd()));

                    } else if (previousRange.getEnd() > range.getStart()) {
                        throw new RuntimeException("Unexpected problem when parsing PDF - visual content ranges overlaps");

                    } else {
                        result.add(range);
                    }
                }
            }

            return new PdfVisualContentDescription(result);
        }

        public void process(PdfObject object) throws IOException {
            if (ranges.containsKey(object)) {
                return;
            }

            if (object.getIndirectReference() != null) {
                processReference(object, object.getIndirectReference());
            }

            if (object instanceof PdfDictionary) {
                processDictionary((PdfDictionary) object);
            }
            if (object instanceof PdfArray) {
                processArray((PdfArray) object);
            }
        }

        private void processDictionary(PdfDictionary dictionary) throws IOException {
            for (PdfName name : dictionary.keySet()) {
                PdfObject object = dictionary.get(name);
                process(object);
            }
        }

        private void processArray(PdfArray array) throws IOException {
            for (PdfObject object : array) {
                process(object);
            }
        }

        private void processReference(PdfObject object, PdfIndirectReference ref) throws IOException {
            if (ref.getOffset() == -1) {
                process(document.getPdfObject(ref.getObjStreamNumber()));
                return;
            }

            tokenizer.seek(ref.getOffset());

            tokenizer.nextValidToken();
            if (tokenizer.getTokenType() != TokenType.Obj) {
                throw new IOException("Unexpected problem when parsing PDF - no 'obj' token at offset " + ref.getOffset());
            }

            do {
                tokenizer.nextValidToken();

                if (isAtStreamToken(tokenizer)) {
                    if (!(object instanceof PdfStream)) {
                        throw new IOException("Unexpected problem when parsing PDF - 'stream' token found for non-stream object");
                    }
                    PdfStream stream = (PdfStream) object;

                    skipToNextLine();
                    tokenizer.seek(tokenizer.getPosition() + stream.getLength());
                }
            } while (!isAtEndObjToken(tokenizer));

            skipToNextLine();

            ranges.put(object, new PdfFileRange(ref.getOffset(), tokenizer.getPosition()));
        }

        private static boolean isAtStreamToken(PdfTokenizer tokenizer) {
            return tokenizer.getTokenType() == TokenType.Other && tokenizer.tokenValueEqualsTo(STREAM);
        }

        private static boolean isAtEndObjToken(PdfTokenizer tokenizer) {
            return tokenizer.getTokenType() == TokenType.EndObj
                    || tokenizer.getTokenType() == TokenType.Other && tokenizer.tokenValueEqualsTo(END_OBJ);
        }

        private void skipToNextLine() throws IOException {
            int c;
            do {
                c = tokenizer.read();
            } while (PdfTokenizer.isWhitespace(c) && !isNewLine(c));

            if (isNewLine(c)) {
                c = tokenizer.read();
            }
            if (!isNewLine(c)) {
                tokenizer.seek(tokenizer.getPosition() - 1);
            }
        }

        private static boolean isNewLine(int character) {
            return character == '\r' || character == '\n';
        }
    }

    public static class PdfVisualContentDescription {

        private final List<PdfFileRange> contentRanges;

        public PdfVisualContentDescription(Collection<PdfFileRange> contentRanges) {
            this.contentRanges = new ArrayList<>(contentRanges);
        }

        public List<PdfFileRange> getContentRanges() {
            return contentRanges;
        }
    }

    public static class PdfFileRange {

        private final long start;
        private final long end;

        public PdfFileRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public long getLength() {
            return end - start;
        }

        @Override
        public String toString() {
            return "PdfFileRange{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }
    }
}
