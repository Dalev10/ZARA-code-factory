package com.codefactory.supplychain.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Activa {@code @Retryable} y {@code @ConcurrencyLimit} (Spring Framework 7) en toda la
 * aplicación.
 *
 * Vive aquí y no en la clase principal para no cargarse en los tests parciales
 * ({@code @DataJpaTest}), igual que {@code @EnableMethodSecurity} en SecurityConfig.
 * Ojo: por defecto {@code @Retryable} reintenta 3 veces con 1 segundo de espera fijo;
 * cada uso debe ajustar {@code delay} a lo que tolere su operación. Los reintentos
 * optimistas (D-24) usan {@code delay = 10, jitter = 10, multiplier = 2}: con espera
 * fija, ventas simultáneas sobre la misma fila volvían a chocar en cada intento.
 * Riesgo aceptado del MVP: con muchas ventas simultáneas del mismo SKU en la misma
 * tienda, una puede agotar los 3 reintentos y responder 409; no se pierden unidades y
 * el cliente reintenta con el mismo transaccion_id sin duplicar.
 */
@Configuration
@EnableResilientMethods
public class ResilienciaConfig {
}
