package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-17 — Modificar Producto. Casos CP-HU17-001 a CP-HU17-003.
 *
 * <p>Ver Javadoc de {@link Hu15RegistrarProductoIT}: la entidad Producto no
 * existe en el backend.
 */
@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver Hu15RegistrarProductoIT).")
class Hu17ModificarProductoIT extends AbstractCasoPruebaIT {

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
