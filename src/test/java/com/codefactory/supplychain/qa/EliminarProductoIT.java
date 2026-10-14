package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
class EliminarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU18-001 — Eliminar Producto sin dependencias bloqueantes")
    void cpHu18001_eliminarProductoSinDependencias() {
    }

    @Test
    @DisplayName("CP-HU18-002 — Bloquear eliminación de Producto con inventario u operaciones asociadas")
    void cpHu18002_bloquearEliminacionConDependencias() {
    }

    @Test
    @DisplayName("CP-HU18-003 — Conservar información histórica después de eliminar un Producto")
    void cpHu18003_conservarInformacionHistorica() {
    }
}
