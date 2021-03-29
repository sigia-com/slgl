package io.slgl.api.document.model;

import eu.europa.esig.dss.detailedreport.jaxb.XmlSignature;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SignatureQualification;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.validation.PAdESSignature;
import eu.europa.esig.dss.pades.validation.PdfSignatureDictionary;
import io.slgl.permission.context.EvaluationContext;
import io.slgl.permission.context.EvaluationContextObject;
import lombok.AllArgsConstructor;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.Principal;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toMap;

public class DocumentSignatureInfo implements EvaluationContextObject {

    private static final EnumSet<SignatureQualification> QUALIFIED_SIGNATURES = EnumSet.of(
            SignatureQualification.QES,
            SignatureQualification.QESIG,
            SignatureQualification.QESEAL
    );

    private final XmlSignature detailedReportSignature;
    private final PAdESSignature signature;
    private final byte[] documentBytes;
    private Boolean doesCoverWholeDocument;

    public DocumentSignatureInfo(PAdESSignature signature, XmlSignature detailedReportSignature, byte[] documentBytes) {
        checkArgument(
                Objects.equals(signature.getId(), detailedReportSignature.getId()),
                "signature have to be of the same id"
        );
        this.detailedReportSignature = detailedReportSignature;
        this.signature = signature;
        this.documentBytes = documentBytes;

    }

    @Override
    public EvaluationContext asEvaluationContext() {
        PdfSignatureDictionary pdfDictionary = signature.getPdfSignatureDictionary();
        return EvaluationContext.builder()
                .value("signature_algorithm", getSignatureAlgorithm())
                .value("encryption_algorithm", getEncryptionAlgorithm())
                .value("digest_algorithm", getDigestAlgorithm())
                .value("mask_generation_function", getMaskGenerationFunction())
                .value("sign_date", getSigningTime())
                .value("validation_passed", isTotalPassed())
                .value("validation_failed", isTotalFailed())

                .provider("signature_qualification", this::getSignatureQualification)
                .provider("is_qualified", this::isQualified)

                .provider("covers_whole_document", this::doesCoverWholeDocument)

                .value("signing_reason", pdfDictionary.getReason())
                .value("signing_location", pdfDictionary.getLocation())
                .value("signer_name", pdfDictionary.getSignerName())
                .value("contact_info", pdfDictionary.getContactInfo())
                .value("filter", pdfDictionary.getFilter())
                .value("sub_filter", pdfDictionary.getSubFilter())

                .provider("certificate", this::certificateInfo)

                .build();
    }

    private Object getSignatureAlgorithm() {
        return signature.getSignatureAlgorithm() == null ? null : signature.getSignatureAlgorithm().name();
    }

    private Object getEncryptionAlgorithm() {
        return signature.getEncryptionAlgorithm() == null ? null : signature.getEncryptionAlgorithm().getName();
    }

    private Object getDigestAlgorithm() {
        return signature.getDigestAlgorithm() == null ? null : signature.getDigestAlgorithm().getName();
    }

    private Object getMaskGenerationFunction() {
        return signature.getMaskGenerationFunction() == null ? null : signature.getMaskGenerationFunction().name();
    }

    private Instant getSigningTime() {
        return signature.getSigningTime() == null ? null : signature.getSigningTime().toInstant();
    }

    private DocumentCertificateInfo certificateInfo() {
        return new DocumentCertificateInfo(signature.getSigningCertificateToken());
    }

    private boolean isQualified() {
        if (isTotalPassed()) {
            var qualification = detailedReportSignature.getValidationSignatureQualification().getSignatureQualification();
            return QUALIFIED_SIGNATURES.contains(qualification);
        } else {
            return false;
        }
    }

    private String getSignatureQualification() {
        return detailedReportSignature.getValidationSignatureQualification().getSignatureQualification().getReadable();
    }

    private boolean isTotalPassed() {
        var conclusionIndication = detailedReportSignature.getConclusion().getIndication();
        return Indication.TOTAL_PASSED.equals(conclusionIndication);
    }

    private boolean isTotalFailed() {
        var conclusionIndication = detailedReportSignature.getConclusion().getIndication();
        return Indication.TOTAL_FAILED.equals(conclusionIndication);
    }

    public boolean hasNotFailed() {
        var conclusionIndication = detailedReportSignature.getConclusion().getIndication();
        return Indication.TOTAL_PASSED.equals(conclusionIndication)
                || Indication.INDETERMINATE.equals(conclusionIndication);
    }

    public boolean doesCoverWholeDocument() {
        if (doesCoverWholeDocument == null) {

            var byteRange = signature.getPdfSignatureDictionary().getByteRange();
            var offset1 = byteRange.getFirstPartStart(); // offset of pdf content
            var length1 = byteRange.getFirstPartEnd(); // pdf content before signature
            var offset2 = byteRange.getSecondPartStart(); // length1 + signature size
            var length2 = byteRange.getSecondPartEnd(); // pdf content after signature
            long endOfContent = offset2 + length2;

            // multiply content length with 2 (because it is in hex in the PDF) and add 2 for '<' and '>' signature delimiters
            int signatureContentLength = signature.getPdfSignatureDictionary().getContents().length * 2 + 2;
            // a false result doesn't necessarily mean that the PDF is a fake
            this.doesCoverWholeDocument = (endOfContent == (long) documentBytes.length)
                    && (offset1 == 0)
                    && (length1 + signatureContentLength == offset2);
        }
        return doesCoverWholeDocument;
    }

    @AllArgsConstructor
    private class DocumentCertificateInfo implements EvaluationContextObject {
        private final CertificateToken certificate;

        @Override
        public EvaluationContext asEvaluationContext() {
            return EvaluationContext.builder()
                    .value("serial_number", certificate.getSerialNumber().toString())
                    .value("issuer", getRdns(certificate.getIssuer().getPrincipal()))
                    .value("subject", getRdns(certificate.getSubject().getPrincipal()))
                    .value("self_signed", certificate.isSelfSigned())
                    .value("self_issued", certificate.isSelfIssued())
                    .value("not_valid_before", certificate.getNotBefore().toInstant())
                    .value("not_valid_after", certificate.getNotAfter().toInstant())
                    .value("valid_now", isValidAt(certificate.getCertificate(), new Date()))
                    .value("valid_at_sign_time", isValidAt(certificate.getCertificate(), signature.getSigningTime()))
                    .build();
        }

        private boolean isValidAt(X509Certificate certificate, Date time) {
            try {
                certificate.checkValidity(time);
                return true;
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                return false;
            }
        }

        private Map<String, String> getRdns(Principal principal) {
            LdapName subjectLdapName;
            try {
                subjectLdapName = new LdapName(principal.toString());
            } catch (InvalidNameException e) {
                throw new DSSException("invalid principal name: " + principal, e);
            }
            return subjectLdapName.getRdns().stream()
                    .collect(toMap(Rdn::getType, rdn -> rdn.getValue().toString()));
        }

    }
}
