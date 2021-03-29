package io.slgl.api.document.service;

import eu.europa.esig.dss.alert.SilentOnStatusAlert;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.validation.PAdESSignature;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignaturePolicyProvider;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import io.slgl.api.ExecutionContext;
import io.slgl.api.document.model.DocumentSignatureInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.slgl.api.utils.StreamUtils.filterInstance;
import static io.slgl.api.utils.TimerUtils.getWithTimer;

@Slf4j
public class DocumentSignatureInfoFactory {
    private final TrustListManagementService trustListManagementService = ExecutionContext.get(TrustListManagementService.class);

    public List<DocumentSignatureInfo> getSignatureInfo(byte[] documentBytes) {
        SignedDocumentValidator validator = getWithTimer("create validator", () -> createValidator(documentBytes));

        var reports = getWithTimer("validate document", validator::validateDocument);
        var signatures = validator.getSignatures();

        return reports.getDetailedReport().getSignatures().stream()
                .flatMap(reportSignature -> signatures.stream()
                        .filter(signature -> Objects.equals(reportSignature.getId(), signature.getId()))
                        .flatMap(filterInstance(PAdESSignature.class))
                        .map(signature -> new DocumentSignatureInfo(signature, reportSignature, documentBytes)))
                .collect(Collectors.toList());

    }

    private SignedDocumentValidator createValidator(byte[] bytes) {
        var document = new InMemoryDocument(bytes);
        var validator = SignedDocumentValidator.fromDocument(document);
        validator.setCertificateVerifier(getCertificateVerifier());
        validator.setSignaturePolicyProvider(new SignaturePolicyProvider());
        return validator;
    }

    private CertificateVerifier getCertificateVerifier() {
        var verifier = new CommonCertificateVerifier();
        verifier.setTrustedCertSources(trustListManagementService.getTrustedListsCertificateSource());
        verifier.setCrlSource(new OnlineCRLSource());
        verifier.setOcspSource(new OnlineOCSPSource());
        verifier.setDataLoader(new CommonsDataLoader());

        var silentAlert = new SilentOnStatusAlert();
        verifier.setAlertOnInvalidTimestamp(silentAlert);
        verifier.setAlertOnUncoveredPOE(silentAlert);
        verifier.setAlertOnNoRevocationAfterBestSignatureTime(silentAlert);
        verifier.setAlertOnMissingRevocationData(silentAlert);
        verifier.setAlertOnRevokedCertificate(silentAlert);

        return verifier;
    }


}
