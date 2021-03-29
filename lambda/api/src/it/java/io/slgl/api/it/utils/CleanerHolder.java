package io.slgl.api.it.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;

public class CleanerHolder {
    private static final Cleaner cleaner = Cleaner.create();
    private static final Logger log = LoggerFactory.getLogger(CleanerHolder.class);

    public static <T extends AutoCloseable> T registerInCleaner(T closable) {
        cleaner.register(cleaner, () -> {
            try {
                closable.close();
            } catch (Exception e) {
                log.error("failed to clean", e);
            }
        });
        return closable;
    }
}
