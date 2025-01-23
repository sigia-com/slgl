package io.slgl.api.document.service;

import com.google.common.util.concurrent.MoreExecutors;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.tsl.alerts.LOTLAlert;
import eu.europa.esig.dss.tsl.alerts.TLAlert;
import eu.europa.esig.dss.tsl.alerts.detections.LOTLLocationChangeDetection;
import eu.europa.esig.dss.tsl.alerts.detections.OJUrlChangeDetection;
import eu.europa.esig.dss.tsl.alerts.detections.TLExpirationDetection;
import eu.europa.esig.dss.tsl.alerts.detections.TLSignatureErrorDetection;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogLOTLLocationChangeAlertHandler;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogOJUrlChangeAlertHandler;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogTLExpirationAlertHandler;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogTLSignatureErrorAlertHandler;
import eu.europa.esig.dss.tsl.cache.CacheCleaner;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.tsl.sync.AcceptAllStrategy;
import io.slgl.api.ExecutionContext;
import io.slgl.api.utils.LambdaEnv;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipInputStream;

import static io.slgl.api.document.service.DssProperties.*;

@Slf4j
public class TrustListManagementService {

    @Getter
    private final TrustedListsCertificateSource trustedListsCertificateSource = new TrustedListsCertificateSource();

    private final TLValidationJob tlValidationJob;
    private final ZipCacheDSSFileLoader loader;
    private final boolean online;
    private Instant lastS3Reloaded;
    private Map<String, DSSZipCacheEntry> lastCacheSnapshot;

    private TrustListManagementService(boolean online, ZipCacheDSSFileLoader loader) {
        this.online = online;
        this.loader = loader;
        tlValidationJob = createTLValidationJob();
    }

    public static TrustListManagementService online(DSSFileLoader loader) {
        Objects.requireNonNull(loader, "online loader has to be provided");
        return new TrustListManagementService(true, new ZipCacheDSSFileLoader(loader));
    }

    public static TrustListManagementService offline() {
        return new TrustListManagementService(false, new ZipCacheDSSFileLoader(null));
    }

    private TLValidationJob createTLValidationJob() {
        var tlValidationJob = new TLValidationJob();
        tlValidationJob.setExecutorService(getExecutorService());
        if (online) {
            tlValidationJob.setOnlineDataLoader(loader);
        } else {
            tlValidationJob.setOfflineDataLoader(loader);
        }
        tlValidationJob.setCacheCleaner(cacheCleaner(loader));
        tlValidationJob.setTrustedListCertificateSource(trustedListsCertificateSource);
        tlValidationJob.setSynchronizationStrategy(new AcceptAllStrategy());

        LOTLSource europeanLOTL = europeanLOTL();
        tlValidationJob.setListOfTrustedListSources(europeanLOTL);
        tlValidationJob.setLOTLAlerts(List.of(
                ojUrlAlert(europeanLOTL),
                lotlLocationAlert(europeanLOTL)
        ));
        tlValidationJob.setTLAlerts(List.of(
                tlSigningAlert(),
                tlExpirationDetection()
        ));
        return tlValidationJob;
    }

    private ExecutorService getExecutorService() {
        var threadCount = LambdaEnv.DssCache.getThreadCount();
        if (threadCount == 0) {
            return MoreExecutors.newDirectExecutorService();
        } else {
            return Executors.newFixedThreadPool(threadCount);
        }
    }

    public void exportCacheToS3IfUpdated() {
        if (!loader.differs(lastCacheSnapshot)) {
            log.info("Cache has not been changed");
            return;
        }
        try {
            var request = PutObjectRequest.builder()
                    .bucket(LambdaEnv.DssCache.getS3Bucket())
                    .key(LambdaEnv.DssCache.getTrustListCacheZipS3Key())
                    .build();
            var bytes = loader.toByteArray();
            S3Client s3Client = S3Client.builder().build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            lastCacheSnapshot = loader.currentSnapshot();
            log.info("Zipped cache exported to s3");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reloadCacheFromS3(boolean throwOnMissingZip) {
        var request = GetObjectRequest.builder()
                .bucket(LambdaEnv.DssCache.getS3Bucket())
                .key(LambdaEnv.DssCache.getTrustListCacheZipS3Key())
                .ifModifiedSince(lastS3Reloaded)
                .build();
        try (
                S3Client s3Client = S3Client.builder().build();
                var response = s3Client.getObject(request);
                var zipInput = new ZipInputStream(response)
        ) {
            loader.initFromZip(zipInput);
            lastS3Reloaded = response.response().lastModified();
            lastCacheSnapshot = loader.currentSnapshot();
        } catch (NoSuchKeyException e) {
            if (throwOnMissingZip) {
                throw new IllegalStateException(e);
            } else {
                log.info("No cached zip found");
            }
        } catch (S3Exception e) {
            if (e.statusCode() == 304) {
                log.info("Cache not modified on s3 since ({}), refresh not needed", lastS3Reloaded);
            } else {
                throw e;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void refresh() {
        if (online) {
            tlValidationJob.onlineRefresh();
        } else {
            tlValidationJob.offlineRefresh();
        }
    }

    private LOTLSource europeanLOTL() {
        LOTLSource lotlSource = new LOTLSource();
        lotlSource.setUrl(LOTL_URL);
        lotlSource.setCertificateSource(officialJournalContentKeyStore());
        lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(OJ_URL));
        lotlSource.setPivotSupport(true);
        return lotlSource;
    }

    private CertificateSource officialJournalContentKeyStore() {
        try (var keystoreStream = getClass().getClassLoader().getResourceAsStream(KS_FILE_RESOURCE)) {
            return new KeyStoreCertificateSource(keystoreStream, KS_TYPE, KS_PASSWORD);
        } catch (Throwable e) {
            throw new DSSException("Unable to load the keystore", e);
        }
    }

    private CacheCleaner cacheCleaner(DSSFileLoader loader) {
        CacheCleaner cacheCleaner = new CacheCleaner();
        cacheCleaner.setCleanMemory(true);
        cacheCleaner.setCleanFileSystem(true);
        cacheCleaner.setDSSFileLoader(loader);
        return cacheCleaner;
    }

    private TLAlert tlSigningAlert() {
        var signingDetection = new TLSignatureErrorDetection();
        var handler = new LogTLSignatureErrorAlertHandler();
        return new TLAlert(signingDetection, handler);
    }

    private TLAlert tlExpirationDetection() {
        var expirationDetection = new TLExpirationDetection();
        var handler = new LogTLExpirationAlertHandler();
        return new TLAlert(expirationDetection, handler);
    }

    private LOTLAlert ojUrlAlert(LOTLSource source) {
        var ojUrlDetection = new OJUrlChangeDetection(source);
        var handler = new LogOJUrlChangeAlertHandler();
        return new LOTLAlert(ojUrlDetection, handler);
    }

    private LOTLAlert lotlLocationAlert(LOTLSource source) {
        var lotlLocationDetection = new LOTLLocationChangeDetection(source);
        var handler = new LogLOTLLocationChangeAlertHandler();
        return new LOTLAlert(lotlLocationDetection, handler);
    }
}
