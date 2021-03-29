package io.slgl.api.observer.model;

import com.google.common.base.Charsets;
import io.slgl.api.domain.UploadedFile;
import io.slgl.api.protocol.NodeResponse;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Getter
@Setter
@Accessors(chain = true)
public class ObserverData {

    private String sourceNode;
    private String targetNode;
    private String targetAnchor;

    private String rawJson;
    private NodeResponse node;

    private String fileName;
    private byte[] fileBytes;

    public byte[] getBytes() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                ZipEntry ledgerEntryEntry = new ZipEntry("node.json");
                zos.putNextEntry(ledgerEntryEntry);
                zos.write(toJson(node).getBytes());
                zos.closeEntry();

                if (rawJson != null) {
                    ZipEntry dataEntry = new ZipEntry("data.json");
                    zos.putNextEntry(dataEntry);
                    zos.write(rawJson.getBytes(Charsets.UTF_8));
                    zos.closeEntry();
                }

                if (fileBytes != null) {
                    ZipEntry documentEntry = new ZipEntry(fileName);
                    zos.putNextEntry(documentEntry);
                    zos.write(fileBytes);
                    zos.closeEntry();
                }
            }

            return baos.toByteArray();
        }
    }

    public String toJson(Object entry) {
        return UncheckedObjectMapper.MAPPER.writeValueAsString(entry);
    }

    public ObserverData setFile(UploadedFile uploadedFile) {
        if (uploadedFile != null) {
            fileName = uploadedFile.isPdf() ? "file.pdf" : "file.raw";
            fileBytes = uploadedFile.getBytes();
        }

        return this;
    }
}
