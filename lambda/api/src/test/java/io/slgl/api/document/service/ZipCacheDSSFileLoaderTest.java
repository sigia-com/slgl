package io.slgl.api.document.service;

import com.google.common.collect.Maps;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.MemoryDataLoader;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ZipCacheDSSFileLoaderTest {

    @Test
    void shouldProperlySerializeAndDeserialize(@TempDir File tmpDir) throws IOException {
        // given
        var availableFiles = Map.of(
                "http://test.com/file", RandomUtils.nextBytes(24_000),
                "http://test.com/file.xml", "<tag>text</value>".getBytes(),
                "https://test.com/file.json", "{ \"key\" : [] }".getBytes(),
                "tcp://test.com/file.json", "{ \"key\" : [] }".getBytes(),
                "file:///some/local/file", "{ \"key\" : [] }".getBytes(),
                "classpath:org/test/package/resource.extension", "whatever".getBytes()
        );

        FileCacheDataLoader rootLoader = new FileCacheDataLoader();
        rootLoader.setDataLoader(new MemoryDataLoader(availableFiles));
        rootLoader.setFileCacheDirectory(tmpDir);

        var docsFromRootLoader = loadDocs(rootLoader, availableFiles.keySet());
        var bytesFromRootLoader = Maps.transformValues(docsFromRootLoader, DSSUtils::toByteArray);

        // when
        ZipCacheDSSFileLoader cachingLoader = new ZipCacheDSSFileLoader(rootLoader);
        var docsFromCachingLoader = loadDocs(cachingLoader, availableFiles.keySet());
        var bytesFromCachingLoader = Maps.transformValues(docsFromCachingLoader, DSSUtils::toByteArray);

        byte[] zippedCache = serialize(cachingLoader);

        ZipCacheDSSFileLoader deserializedLoader = deserialize(zippedCache);
        var docsFromDeserializedLoader = loadDocs(deserializedLoader, availableFiles.keySet());
        var bytesFromDeserializedLoader = Maps.transformValues(docsFromDeserializedLoader, DSSUtils::toByteArray);

        // then
        assertSoftly(softly -> {
            softly.assertThat(bytesFromDeserializedLoader)
                    .containsAllEntriesOf(bytesFromCachingLoader)
                    .containsAllEntriesOf(bytesFromRootLoader)
                    .containsAllEntriesOf(availableFiles);

            softly.assertThat(docsFromDeserializedLoader)
                    .containsExactlyEntriesOf(docsFromCachingLoader)
                    .containsExactlyEntriesOf(docsFromRootLoader);
        });

    }

    private ZipCacheDSSFileLoader deserialize(byte[] zippedCache) throws IOException {
        var deserializedLoader = new ZipCacheDSSFileLoader();
        var input = new ZipInputStream(new ByteArrayInputStream(zippedCache));
        deserializedLoader.initFromZip(input);
        return deserializedLoader;
    }

    private byte[] serialize(ZipCacheDSSFileLoader cachingLoader) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        cachingLoader.writeTo(new ZipOutputStream(outputStream));
        return outputStream.toByteArray();
    }

    private Map<String, DSSDocument> loadDocs(DSSFileLoader loader, Collection<String> urls) {
        return urls.stream().collect(toMap(
                identity(),
                url -> new ComparableDocument(loader.getDocument(url)))
        );
    }

    @AllArgsConstructor
    private static class ComparableDocument implements DSSDocument {
        @Delegate
        private final DSSDocument document;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComparableDocument that = (ComparableDocument) o;
            return Objects.equals(document.getName(), that.document.getName())
                    && Objects.equals(document.getMimeType(), that.document.getMimeType())
                    && Arrays.equals(DSSUtils.toByteArray(document), DSSUtils.toByteArray(that.document));
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName());
        }

        @Override
        public String toString() {
            return String.format(
                    "ComparableDocument{name=`%s` mimeType=`%s` bytes=`%d`}",
                    document.getName(), document.getMimeType(), Arrays.hashCode(DSSUtils.toByteArray(document))
            );
        }
    }

}