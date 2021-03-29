package io.slgl.trustlistrefresher.config;

import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import io.slgl.api.ExecutionContext;
import io.slgl.api.ExecutionContextModule;
import io.slgl.api.document.service.TrustListManagementService;
import io.slgl.api.document.service.ZipCacheDSSFileLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class TLCacheRefresherHandlerModule implements ExecutionContextModule {

    @Override
    public void configure() {
        ExecutionContext.put(
                TrustListManagementService.class,
                TrustListManagementService.online(defaultLoader())
        );
    }

    private DSSFileLoader defaultLoader() {
        var onlineFileLoader = new FileCacheDataLoader();
        onlineFileLoader.setCacheExpirationTime(0);
        onlineFileLoader.setDataLoader(new CommonsDataLoader());
        onlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
        return new ZipCacheDSSFileLoader(onlineFileLoader);
    }

    private File tlCacheDirectory() {
        File rootFolder = new File(System.getProperty("java.io.tmpdir"));
        File tslCache = new File(rootFolder, "dss-tsl-loader");
        if (tslCache.mkdirs()) {
            log.info("TL Cache folder : {}", tslCache.getAbsolutePath());
        }
        return tslCache;
    }
}
