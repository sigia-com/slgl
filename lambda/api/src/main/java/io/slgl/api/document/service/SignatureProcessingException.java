package io.slgl.api.document.service;

import io.slgl.api.permission.model.EvaluationLogCodes;
import lombok.Getter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.bouncycastle.cms.CMSVerifierCertificateNotValidException;
import org.bouncycastle.operator.OperatorCreationException;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.util.Optional;
import java.util.stream.Stream;

import static io.slgl.api.permission.model.EvaluationLogCodes.*;

public class SignatureProcessingException extends Exception {

    @Getter
    private final String code;

    private SignatureProcessingException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public Throwable fillInStackTrace() {
        // we don't need expensive stacktrace in informative exceptions
        return this;
    }

    public static SignatureProcessingException invalidByteRangeLength(int length) {
        return new SignatureProcessingException(SIGNATURE_MALFORMED, "Signature byteRange must contains 4 integers, but had: " + length);
    }

    public static SignatureProcessingException corruptedPdf() {
        return new SignatureProcessingException(EvaluationLogCodes.CORRUPTED_PDF, "Cannot read PDF file content");
    }

    static SignatureProcessingException cmsException(CMSException e) {
        if (e instanceof CMSSignerDigestMismatchException) {
            return new SignatureProcessingException(SIGNER_DIGEST_MISMATCH, e.getMessage());
        }
        if (e instanceof CMSVerifierCertificateNotValidException) {
            return new SignatureProcessingException(CERTIFICATE_NOT_VALID, e.getMessage());
        }
        return unknown(e);
    }

    static SignatureProcessingException operationCreationException(OperatorCreationException exception) {
        return convert(exception);
    }

    static SignatureProcessingException missingSubFilter() {
        return new SignatureProcessingException(MISSING_SUB_FILTER, "One of the signatures has no subFilter.");
    }

    static SignatureProcessingException unsupportedSubFilter(String subFilterName) {
        return new SignatureProcessingException(UNSUPPORTED_SUB_FILTER, String.format("PDF certificate's subFilter: `%s` is not supported", subFilterName));
    }

    static SignatureProcessingException malformedSignature() {
        return new SignatureProcessingException(SIGNATURE_MALFORMED, "Signature has been malformed");
    }

    public static SignatureProcessingException invalidDN(Principal principal) {
        return new SignatureProcessingException(INVALID_CERTIFICATE_DN, "Invalid distinguished name:" + principal.getName());
    }

    static SignatureProcessingException noSuchProvider(NoSuchProviderException e) {
        return new SignatureProcessingException(UNSUPPORTED_PROVIDER, e.getMessage());
    }

    static SignatureProcessingException noSuchAlgorithm(NoSuchAlgorithmException e) {
        return new SignatureProcessingException(UNSUPPORTED_ALGORITHM, e.getMessage());
    }

    public static SignatureProcessingException convert(Throwable exception) {
        return Stream.iterate(exception, it -> it.getCause() != null, Throwable::getCause)
                .map(SignatureProcessingException::tryConvert)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> unknown(exception));
    }

    private static Optional<SignatureProcessingException> tryConvert(Throwable exception) {
        if (exception instanceof CMSException) {
            return Optional.of(cmsException(((CMSException) exception)));
        }
        if (exception instanceof NoSuchProviderException) {
            return Optional.of(noSuchProvider(((NoSuchProviderException) exception)));
        }
        if (exception instanceof NoSuchAlgorithmException) {
            return Optional.of(noSuchAlgorithm(((NoSuchAlgorithmException) exception)));
        }

        return Optional.empty();
    }

    static SignatureProcessingException unknown(Throwable e) {
        return new SignatureProcessingException(UNKNOWN, e.getMessage());
    }
}
