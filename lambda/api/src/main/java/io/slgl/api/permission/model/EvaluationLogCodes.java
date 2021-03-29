package io.slgl.api.permission.model;

public abstract class EvaluationLogCodes {
    public static String UNKNOWN = "unknown";

    public static String ANCHOR_MAX_SIZE_EXCEEDED = "anchor_max_size_exceeded";
    public static String SIGNATURE_MALFORMED = "signature_malformed";
    public static String MISSING_SUB_FILTER = "missing_sub_filter";
    public static String UNSUPPORTED_SUB_FILTER = "unsupported_sub_filter";
    public static String INVALID_CERTIFICATE_DN = "invalid_certificate_dn";
    public static String UNSUPPORTED_ALGORITHM = "unsupported_algorithm";
    public static String UNSUPPORTED_PROVIDER = "unsupported_provider";
    public static String SIGNER_DIGEST_MISMATCH = "signer_digest_mismatch";
    public static String CERTIFICATE_NOT_VALID = "certificate_not_valid";
    public static String CORRUPTED_PDF = "corrupted_pdf";
}
