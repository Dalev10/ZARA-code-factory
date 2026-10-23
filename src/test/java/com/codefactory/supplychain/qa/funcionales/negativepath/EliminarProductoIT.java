package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
@Tag("funcional")
@Tag("negative-path")
class EliminarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-002 — Bloquear eliminación de Producto con inventario u operaciones asociadas")
    void cpHu18002_bloquearEliminacionConDependencias() {
    }
}
