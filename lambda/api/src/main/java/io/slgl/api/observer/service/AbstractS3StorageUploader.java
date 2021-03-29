package io.slgl.api.observer.service;

import com.google.common.base.Charsets;
import io.slgl.api.observer.model.ErrorResult;
import io.slgl.api.observer.model.ObserverData;
import io.slgl.api.observer.model.Result;
import io.slgl.api.observer.model.S3SuccessResult;
import io.slgl.api.utils.SupplierWithException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static io.slgl.api.utils.Utils.concatenateAsUrlPartsWithSlash;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public abstract class AbstractS3StorageUploader implements StorageUploader {

    protected final PgpEncrypter pgpEncrypter;

    AbstractS3StorageUploader(PgpEncrypter pgpEncrypter) {
        this.pgpEncrypter = pgpEncrypter;
    }

    protected Result uploadData(S3Client client, ObserverData observerData, String pgpPublicKey, String bucketName, String key) {
        return handleErrors(() -> {
            var serializedData = pgpEncrypter.encrypt(observerData.getBytes(), pgpPublicKey);
            var request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentLength((long) serializedData.length)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            var result = client.putObject(request, RequestBody.fromBytes(serializedData));

            return new S3SuccessResult(bucketName, key, result.versionId(), result.eTag());
        });
    }

    private Result handleErrors(SupplierWithException<Result, Exception> supplier) {
        try {
            return supplier.apply();
        } catch (AwsServiceException e) {
            return new ErrorResult("Errors occurred in Amazon S3 while processing the request", e);
        } catch (SdkClientException e) {
            return new ErrorResult("Errors occurred in the client while making the request or handling the response", e);
        } catch (IOException e) {
            return new ErrorResult("Errors occurred during data serialization", e);
        } catch (Exception e) {
            return new ErrorResult("Unknown error during sending data by observer", e);
        }
    }

    protected String buildPath(String pathPrefix, ObserverData observerData) {
        return concatenateAsUrlPartsWithSlash(
                pathPrefix,
                escapeIdPart(observerData.getTargetNode()),
                escapeIdPart(observerData.getTargetAnchor()),
                escapeIdPart(observerData.getSourceNode()) + ".zip.pgp");
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
            throw new IllegalArgumentException("Unsupported characters in encoded text", e);
        }
    }
}
