package io.slgl.api.document.service;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipCacheDSSFileLoader implements DSSFileLoader {

    private final Map<String, DSSZipCacheEntry> cacheMap = new HashMap<>();
    private final DSSFileLoader delegate;

    public ZipCacheDSSFileLoader() {
        this(null);
    }

    public ZipCacheDSSFileLoader(DSSFileLoader delegate) {
        this.delegate = delegate;
    }

    public void initFromZip(ZipInputStream input) throws IOException {
        cacheMap.clear();
        ZipEntry entry;
        while ((entry = input.getNextEntry()) != null) {
            var cacheEntry = DSSZipCacheEntry.read(input, entry);
            var name = DSSUtils.getNormalizedString(cacheEntry.getName());
            cacheMap.put(name, cacheEntry);
        }
    }

    public Map<String, DSSZipCacheEntry> currentSnapshot() {
        return Map.copyOf(cacheMap);
    }

    @Override
    public DSSDocument getDocument(String url) throws DSSException {
        var normalizedUrl = DSSUtils.getNormalizedString(url);
        var cached = cacheMap.get(normalizedUrl);
        if (delegate != null) {
            var loadedDoc = delegate.getDocument(url);
            cached = new DSSZipCacheEntry(loadedDoc);
            putCacheEntry(cached);
        }
        if (cached != null) {
            return cached.asDocument();
        }
        throw new DSSException("Cannot retrieve data from URL [" + url + "]");
    }

    public void writeTo(ZipOutputStream output) throws IOException {
        for (DSSZipCacheEntry entry : cacheMap.values()) {
            entry.writeTo(output);
        }
    }

    public byte[] toByteArray() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zippedOut = new ZipOutputStream(out)) {
            for (DSSZipCacheEntry entry : cacheMap.values()) {
                entry.writeTo(zippedOut);
            }
        }
        return out.toByteArray();
    }

    private void putCacheEntry(DSSZipCacheEntry cacheEntry) {
        var name = DSSUtils.getNormalizedString(cacheEntry.getName());
        cacheMap.put(name, cacheEntry);
    }

    @Override
    public boolean remove(String url) {
        return cacheMap.remove(url) != null;
    }

    public boolean differs(Map<String, DSSZipCacheEntry> lastCacheSnapshot) {
        return !cacheMap.equals(lastCacheSnapshot);
    }
}
