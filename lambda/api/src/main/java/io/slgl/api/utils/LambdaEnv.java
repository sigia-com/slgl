package io.slgl.api.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.Integer.parseInt;

public class LambdaEnv {

    private static final Map<String, String> values = new HashMap<>();

    private LambdaEnv() {
    }

    public static String getSlglQldbLedger() {
        return get("SLGL_QLDB_LEDGER");
    }

    public static String getUserDataDynamoDbTable() {
        return get("USER_DATA_DYNAMO_DB_TABLE");
    }

    public static int getInitialUserCredits() {
        var strValue = get("USER_CREDITS_INITIAL_AMOUNT", String.valueOf(Integer.MAX_VALUE));
        return Integer.parseUnsignedInt(strValue);
    }

    public static String getEntriesSnsTopic() {
        return get("SLGL_SNS_TOPIC");
    }

    private static String get(String key) {
        return Objects.requireNonNull(get(key, null), key + " is not set in environment");
    }

    private static String get(String key, String defaultValue) {
        return values.computeIfAbsent(key, name -> {
            var env = System.getenv(name);
            return env != null ? env : defaultValue;
        });
    }

    public static void override(Map<String, String> values) {
        if (values == null) {
            return;
        }

        LambdaEnv.values.putAll(values);
    }

    public static class DssCache {
        public static String getS3Bucket() {
            return get("DSS_CACHE_S3_BUCKET");
        }

        public static String getTrustListCacheZipS3Key() {
            return get("DSS_CACHE_TRUST_LIST_CACHE_ZIP_S3_KEY", "dss_tl_cache.zip");
        }

        public static int getOfflineReloadRateInMinutes() {
            var minutes = get("DDS_CACHE_OFFLINE_RELOAD_RATE_IN_MINUTES", "30");
            return parseInt(minutes);
        }

        public static int getThreadCount() {
            var count = get("DDS_CACHE_REFRESH_THREAD_COUNT", "2");
            return parseInt(count);
        }
    }

    public static class S3ObserverRecovery {
        public static String getStorageBucket() {
            return get("S3_OBSERVER_DEAD_LETTER_BUCKET");
        }

        public static String getStorageRegion() {
            return get("S3_OBSERVER_DEAD_LETTER_REGION");
        }
    }
}
