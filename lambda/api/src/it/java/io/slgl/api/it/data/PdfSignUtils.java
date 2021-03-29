package io.slgl.api.it.data;

import com.google.common.base.Charsets;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class PdfSignUtils {

    private PdfSignUtils() {
    }

    public static byte[] signPdf(byte[] pdf) {
        return signPdf(pdf, "slgl_integration_test");
    }

    public static byte[] signPdf(byte[] pdf, String certificateAndKeyName) {
        try {
            PAdESService service = new PAdESService(new CommonCertificateVerifier());

            OnlineTSPSource tspSource = new OnlineTSPSource("http://time.certum.pl/");
            tspSource.setDataLoader(new TimestampDataLoader());
            service.setTspSource(tspSource);

            X509Certificate certificate = loadCertificate(certificateAndKeyName);
            RSAPrivateKey privateKey = loadKey(certificateAndKeyName);

            PAdESSignatureParameters signatureParameters = new PAdESSignatureParameters();
            signatureParameters.setSignaturePackaging(SignaturePackaging.DETACHED);
            signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
            signatureParameters.setSigningCertificate(new CertificateToken(certificate));
            signatureParameters.setCertificateChain(new CertificateToken(certificate));

            DSSDocument documentToSign = new InMemoryDocument(pdf);
            ToBeSigned dataToSign = service.getDataToSign(documentToSign, signatureParameters);
            signatureParameters.setSignedData(dataToSign.getBytes());

            SignatureValue signatureValue = sign(dataToSign, certificate, privateKey);

            DSSDocument document = service.signDocument(documentToSign, signatureParameters, signatureValue);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.writeTo(outputStream);
            outputStream.flush();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SignatureValue sign(ToBeSigned dataToSign, X509Certificate certificate, RSAPrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(certificate.getSigAlgName());
            signature.initSign(privateKey);
            signature.update(dataToSign.getBytes());

            byte[] signatureBytes = signature.sign();

            SignatureValue signatureValue = new SignatureValue();
            signatureValue.setAlgorithm(SignatureAlgorithm.forOID(certificate.getSigAlgOID()));
            signatureValue.setValue(signatureBytes);

            return signatureValue;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static X509Certificate loadCertificate(String name) {
        try (InputStream stream = PdfSignUtils.class.getResourceAsStream("/signing/" + name + ".crt")) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            return (X509Certificate) certificateFactory.generateCertificate(stream);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static RSAPrivateKey loadKey(String name) {
        try (InputStream stream = PdfSignUtils.class.getResourceAsStream("/signing/" + name + ".key")) {
            PemReader pemReader = new PemReader(new InputStreamReader(stream, Charsets.UTF_8));
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(content);
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
