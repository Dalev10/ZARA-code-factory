package com.codefactory.supplychain.shared.security;

/**
 * Valores del claim "typ" que distinguen los distintos tipos de JWT que emite el
 * sistema. Existe para que un token de un tipo nunca pueda hacerse pasar por otro
 * — en particular, que un token de desafío MFA (de vida muy corta, emitido tras
 * validar la contraseña pero ANTES del segundo factor) no sirva como access token
 * para llamar a endpoints protegidos.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
public final class JwtClaimTypes {

    public static final String CLAIM_TIPO = "typ";
    public static final String TIPO_ACCESO = "access";
    public static final String TIPO_MFA_CHALLENGE = "mfa_challenge";

    private JwtClaimTypes() {
    }
}
