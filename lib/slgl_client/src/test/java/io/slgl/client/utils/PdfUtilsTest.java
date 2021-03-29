package io.slgl.client.utils;

import io.slgl.client.utils.PdfUtils.PdfFileRange;
import io.slgl.client.utils.PdfUtils.PdfVisualContentDescription;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfUtilsTest {

    @Test
    public void shouldReturnPdfContentHash() throws Exception {
        // given
        byte[] pdf = loadResource("example-stamped.pdf");

        // when
        String hash = PdfUtils.getVisualContentHashHex(pdf);

        // then
        assertThat(hash).isEqualTo("270a4a1873cab95ef33270911262f27aeb3f1a21e9d70e45fa7ad84d097e4789a40fbe365ae8b25bb7a12f671757e63d95980d338b00b607db7c82e686831c2c");
    }

    @Test
    public void shouldReturnTheSameContentHashForSignedPdf() throws Exception {
        // given
        byte[] pdf = loadResource("example-stamped-signed.pdf");

        // when
        String hash = PdfUtils.getVisualContentHashHex(pdf);

        // then
        String originalContentHash = PdfUtils.getVisualContentHashHex(loadResource("example-stamped.pdf"));
        assertThat(hash).isEqualTo(originalContentHash);
    }

    @Test
    public void shouldReturnDifferentContentHashForModifiedPdf() throws Exception {
        // given
        byte[] pdf = loadResource("example.pdf");

        // when
        String hash = PdfUtils.getVisualContentHashHex(pdf);

        // then
        String originalContentHash = PdfUtils.getVisualContentHashHex(loadResource("example-stamped.pdf"));
        assertThat(hash).isNotEqualTo(originalContentHash);
    }

    @Test
    public void shouldReturnNonOverlappingRanges() throws Exception {
        // given
        byte[] pdf = loadResource("example-stamped.pdf");

        // when
        PdfVisualContentDescription result = PdfUtils.getVisualContentDescription(pdf);

        // then
        List<PdfFileRange> ranges = result.getContentRanges();

        for (PdfFileRange range : ranges) {
            assertThat(range.getEnd() > range.getStart())
                    .describedAs("end is after start").isTrue();
        }

        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                PdfFileRange first = ranges.get(i);
                PdfFileRange second = ranges.get(j);

                assertThat(first.getStart() > second.getEnd() || second.getStart() > first.getEnd())
                        .describedAs("ranges doesn't overlap: %s & %s", first, second).isTrue();
            }
        }
    }

    private byte[] loadResource(String resource) {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            return IOUtils.toByteArray(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
