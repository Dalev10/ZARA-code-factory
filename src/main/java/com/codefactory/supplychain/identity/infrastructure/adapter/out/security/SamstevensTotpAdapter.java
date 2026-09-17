package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.TotpPort;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class SamstevensTotpAdapter implements TotpPort {

    private static final String ISSUER = "SupplyChainMVP";
    /** Tolera 1 paso de 30s hacia adelante/atrás para absorber pequeños desfaces de reloj. */
    private static final int DESFACE_PASOS_PERMITIDO = 1;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier;

    public SamstevensTotpAdapter() {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        verifier.setAllowedTimePeriodDiscrepancy(DESFACE_PASOS_PERMITIDO);
        this.codeVerifier = verifier;
    }

    @Override
    public String generarSecreto() {
        return secretGenerator.generate();
    }

    @Override
    public String generarQrDataUri(String secretoPlano, String emailUsuario) {
        QrData data = new QrData.Builder()
                .label(emailUsuario)
                .secret(secretoPlano)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] imagen = qrGenerator.generate(data);
            String base64 = Base64.getEncoder().encodeToString(imagen);
            return "data:" + qrGenerator.getImageMimeType() + ";base64," + base64;
        } catch (QrGenerationException e) {
            throw new IllegalStateException("No se pudo generar el código QR de MFA", e);
        }
    }

    @Override
    public boolean verificarCodigo(String secretoPlano, String codigo) {
        return codeVerifier.isValidCode(secretoPlano, codigo);
    }
}
