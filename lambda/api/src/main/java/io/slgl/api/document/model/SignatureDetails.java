package io.slgl.api.document.model;

import io.slgl.api.document.service.SignatureProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.slgl.api.document.service.SignatureProcessingException.invalidDN;

@Getter
@Setter
@Accessors(chain = true)
public class SignatureDetails {
    private String signAlgorithm;
    private Instant notValidBefore;
    private Instant notValidAfter;
    private String serial;

    private boolean selfSigned;
    private boolean signatureVerified;
    private boolean certificateRevoked;
    private boolean validNow;
    private boolean validAtSignTime;

    private Map<String, String> issuer = new HashMap<>();
    private Map<String, String> subject = new HashMap<>();

    public SignatureDetails setNotValidBefore(Date notValidBefore) {
        this.notValidBefore = toInstant(notValidBefore);
        return this;
    }

    public SignatureDetails setNotValidAfter(Date notValidAfter) {
        this.notValidAfter = toInstant(notValidAfter);
        return this;
    }

    public SignatureDetails setIssuerAndSubject(X509Certificate certFromSignedData) throws SignatureProcessingException {
        LdapName issuerLdapName = toLdapName(certFromSignedData.getIssuerDN());
        LdapName subjectLdapName = toLdapName(certFromSignedData.getSubjectDN());

        for (Rdn rdn : issuerLdapName.getRdns()) {
            issuer.put(rdn.getType(), rdn.getValue().toString());
        }
        for (Rdn rdn : subjectLdapName.getRdns()) {
            subject.put(rdn.getType(), rdn.getValue().toString());
        }
        return this;
    }

    private LdapName toLdapName(Principal subjectDN) throws SignatureProcessingException {
        LdapName subjectLdapName;
        try {
            subjectLdapName = new LdapName(subjectDN.toString());
        } catch (InvalidNameException e) {
            throw invalidDN(subjectDN);
        }
        return subjectLdapName;
    }

    private Instant toInstant(Date date) {
        if (date != null) {
            return date.toInstant();
        }
        return null;
    }
}
