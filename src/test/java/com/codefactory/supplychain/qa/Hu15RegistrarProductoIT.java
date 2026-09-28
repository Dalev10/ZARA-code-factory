package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-15 — Registrar Producto. Casos CP-HU15-001 a CP-HU15-004.
 *
 * <p>Ninguno de los 4 casos de esta historia es ejecutable: el backend no
 * implementa una entidad "Producto". Su catálogo real está modelado como
 * Categoría → Template → Variante (ver {@code catalogo.domain.model}), sin
 * ningún endpoint "/productos". Por decisión explícita, estos casos no se
 * adaptan a Variante/Template/Categoría — quedan documentados como no
 * ejecutables, tal cual están en el documento original.
 */
@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver Javadoc de la clase).")
class Hu15RegistrarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU15-001 — Registrar Producto con información válida y situación definida")
    void cpHu15001_registrarProductoValido() {
    }

    @Test
    @DisplayName("CP-HU15-002 — Rechazar registro de Producto con información obligatoria incompleta")
    void cpHu15002_rechazarInformacionIncompleta() {
    }

    @Test
    @DisplayName("CP-HU15-003 — Rechazar registro de Producto duplicado")
    void cpHu15003_rechazarProductoDuplicado() {
    }

    @Test
    @DisplayName("CP-HU15-004 — Relacionar un Producto con las ubicaciones donde se gestiona")
    void cpHu15004_relacionarProductoConUbicaciones() {
    }
}
