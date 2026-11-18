package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Eventos de autenticación que permiten detectar fuerza bruta (OWASP A09). Van a un logger
 * propio, "seguridad", para filtrarlos o alertar sobre ellos sin el resto del log. La cuenta
 * se identifica por su id: nunca se registran contraseñas, códigos ni el email intentado.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
final class RegistroSeguridad {

    private static final Logger LOG = LoggerFactory.getLogger("seguridad");

    private RegistroSeguridad() {
    }

    /** {@code cuenta} es la ya actualizada por {@link Usuario#registrarIntentoFallido}. */
    static void intentoFallido(Usuario cuenta, String factor, Instant ahora) {
        if (cuenta.estaBloqueadoTemporalmente(ahora)) {
            LOG.warn("Cuenta {} bloqueada hasta {} tras {} intentos fallidos ({})",
                    cuenta.getId(), cuenta.getBloqueadoHasta(), cuenta.getIntentosFallidos(), factor);
        } else {
            LOG.warn("Intento fallido ({}) en la cuenta {}: {} consecutivos",
                    factor, cuenta.getId(), cuenta.getIntentosFallidos());
        }
    }

    static void rechazoPorBloqueo(Usuario cuenta) {
        LOG.warn("Inicio de sesión rechazado: la cuenta {} está bloqueada hasta {}",
                cuenta.getId(), cuenta.getBloqueadoHasta());
    }

    static void emailNoRegistrado() {
        LOG.warn("Intento de inicio de sesión con un email no registrado");
    }
}
