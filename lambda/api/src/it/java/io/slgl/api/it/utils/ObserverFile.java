package io.slgl.api.it.utils;

import com.amazonaws.SdkClientException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.node.LinkResponse;
import io.slgl.client.node.NodeResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.ByteStreams.toByteArray;
import static io.slgl.api.it.properties.Props.getSlglProperties;
import static io.slgl.api.utils.Utils.concatenateAsUrlPartsWithSlash;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.pdfbox.pdmodel.PDDocument.load;

@Slf4j
public class ObserverFile {

    private final String bucket;
    private final String path;

    private final PgpDecrypter pgpDecrypter = new PgpDecrypter();
    private final ObjectMapper objectMapper = UncheckedObjectMapper.MAPPER;

    public ObserverFile(String bucket, String pathPrefix, LinkResponse linkResponse) {
        this.bucket = bucket;
        this.path = concatenateAsUrlPartsWithSlash(
                pathPrefix,
                escapeIdPart(linkResponse.getTargetNode()),
                escapeIdPart(linkResponse.getTargetAnchor()),
                escapeIdPart(linkResponse.getSourceNode()) + ".zip.pgp");
    }

    public ObserverFile(String bucket, String folder, LinkResponse linkResponse, Camouflage camouflage) {
        this.bucket = bucket;
        this.path = concatenateAsUrlPartsWithSlash(
                folder,
                escapeIdPart(linkResponse.getTargetNode()),
                escapeIdPart(camouflage.getOriginalAnchor(linkResponse.getTargetAnchor())),
                escapeIdPart(linkResponse.getSourceNode()) + ".zip.pgp");
    }

    public Optional<DataUploadedToS3> get(String privateKey, String passphrase) {
        var s3Client = S3Client.builder()
                .region(getRegionFromBucket())
                .build();
        try {
            var getObject = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build();

            InputStream object = s3Client.getObject(getObject);

            var tmpFile = File.createTempFile("s3-files", "");
            tmpFile.deleteOnExit();
            Files.copy(
                    new ByteArrayInputStream(pgpDecrypter.decrypt(toByteArray(object), privateKey, passphrase)),
                    tmpFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            return Optional.of(unpackZipFile(tmpFile));

        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DataUploadedToS3 unpackZipFile(File tmpFile) throws IOException {
        var zipFile = new ZipFile(tmpFile);
        DataUploadedToS3.DataUploadedToS3Builder result = DataUploadedToS3.builder();
        try (FileInputStream fis = new FileInputStream(tmpFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {

            List<String> filesInZip = new ArrayList<>();

            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                filesInZip.add(ze.getName());

                if (ze.getName().endsWith("node.json")) {
                    checkState(result.nodeJson == null, "Multiple data.json files in zip!");
                    result.nodeJson(readJson(zipFile, ze, NodeResponse.class));
                } else if (ze.getName().endsWith("data.json")) {
                    checkState(result.objectJson == null, "Multiple data.json files in zip!");
                    result.objectJson(readJson(zipFile, ze));
                } else if (ze.getName().endsWith(".pdf")) {
                    if (result.pdfText != null) {
                        throw new RuntimeException("Multiple pdf files in zip!");
                    }
                    var inputStream = zipFile.getInputStream(ze);
                    result.pdfText(getPdfText(inputStream));
                } else {
                    throw new RuntimeException("Don't know how to handle file: " + ze.getName());
                }
            }

            log.info("Files found in observer ZIP: {}", Joiner.on(", ").join(filesInZip));
        }

        return result.build();
    }

    private Map<String, Object> readJson(ZipFile zipFile, ZipEntry ze) throws IOException {
        var inputStream = zipFile.getInputStream(ze);
        return objectMapper.readValue(IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()), new TypeReference<>() {});
    }

    private <T> T readJson(ZipFile zipFile, ZipEntry ze, Class<T> clazz) throws IOException {
        var inputStream = zipFile.getInputStream(ze);
        return objectMapper.readValue(IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()), clazz);
    }

    private String escapeIdPart(String input) {
        if (isBlank(input)) {
            return "";
        }
        var raw = input
                .replaceAll("/+$", "");
        try {
            return URLEncoder.encode(raw, Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getPdfText(InputStream stream) {
        try {
            var builder = new StringBuilder();
            try (PDDocument document = load(stream)) {
                var pdfStripper = new PDFTextStripper();
                var parsedText = pdfStripper.getText(document);
                builder.append(parsedText);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Region getRegionFromBucket() {
        if (Objects.equal(bucket, getSlglProperties().getObserverStorageS3Bucket())) {
            return Region.of(getSlglProperties().getObserverStorageS3Region());
        } else if (Objects.equal(bucket, getSlglProperties().getObserverDeadLetterBucket())) {
            return Region.of(getSlglProperties().getObserverDeadLetterRegion());
        } else {
            throw new RuntimeException("Unexpected bucket: " + bucket);
        }
    }

    @Value
    @Builder
    public static class DataUploadedToS3 {
        private String pdfText;
        private Map<String, Object> objectJson;
        private NodeResponse nodeJson;
    }
}
