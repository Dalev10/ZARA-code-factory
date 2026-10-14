package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver Javadoc de la clase).")
class RegistrarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Registrar Producto con información válida y situación definida")
    void cpHu15001_registrarProductoValido() {
    }

    @Test
    @DisplayName("CP-002 — Rechazar registro de Producto con información obligatoria incompleta")
    void cpHu15002_rechazarInformacionIncompleta() {
    }

    @Test
    @DisplayName("CP-003 — Rechazar registro de Producto duplicado")
    void cpHu15003_rechazarProductoDuplicado() {
    }

    @Test
    @DisplayName("CP-004 — Relacionar un Producto con las ubicaciones donde se gestiona")
    void cpHu15004_relacionarProductoConUbicaciones() {
    }
}
