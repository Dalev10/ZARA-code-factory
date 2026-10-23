package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver Javadoc de la clase).")
@Tag("funcional")
@Tag("happy-path")
class RegistrarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Registrar Producto con información válida y situación definida")
    void cpHu15001_registrarProductoValido() {
    }

    @Test
    @DisplayName("CP-004 — Relacionar un Producto con las ubicaciones donde se gestiona")
    void cpHu15004_relacionarProductoConUbicaciones() {
    }
}
