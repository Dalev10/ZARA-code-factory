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
 * cada uso debe ajustar {@code delay} a lo que tolere su operación.
 */
@Configuration
@EnableResilientMethods
public class ResilienciaConfig {
}
