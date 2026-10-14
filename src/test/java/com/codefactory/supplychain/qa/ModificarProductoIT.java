package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
class ModificarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU17-001 — Modificar campos permitidos de un Producto")
    void cpHu17001_modificarCamposPermitidos() {
    }

    @Test
    @DisplayName("CP-HU17-002 — Rechazar modificación inválida de un Producto")
    void cpHu17002_rechazarModificacionInvalida() {
    }

    @Test
    @DisplayName("CP-HU17-003 — Reflejar los cambios del Producto en inventario y operaciones asociadas")
    void cpHu17003_reflejarCambiosEnInventarioYOperaciones() {
    }
}
