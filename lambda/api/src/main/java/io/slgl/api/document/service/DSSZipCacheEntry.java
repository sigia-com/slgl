package io.slgl.api.document.service;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.spi.DSSUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Getter
@AllArgsConstructor
public class DSSZipCacheEntry {
    private final byte[] bytes;
    private final String name;

    DSSZipCacheEntry(DSSDocument document) {
        this.bytes = DSSUtils.toByteArray(document);
        this.name = document.getName();
    }

    public DSSDocument asDocument() {
        return new InMemoryDocument(bytes, name);
    }

    public void writeTo(ZipOutputStream outputStream) throws IOException {
        var zipEntry = new ZipEntry(name);
        outputStream.putNextEntry(zipEntry);
        outputStream.write(bytes);
        outputStream.closeEntry();
    }

    public static DSSZipCacheEntry read(ZipInputStream inputStream, ZipEntry zipEntry) throws IOException {
        var bytes = inputStream.readAllBytes();
        return new DSSZipCacheEntry(bytes, zipEntry.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DSSZipCacheEntry that = (DSSZipCacheEntry) o;
        return Arrays.equals(bytes, that.bytes)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }
}
