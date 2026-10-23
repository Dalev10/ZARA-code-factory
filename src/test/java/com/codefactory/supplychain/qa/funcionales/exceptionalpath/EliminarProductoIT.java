package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
@Tag("funcional")
@Tag("exceptional-path")
class EliminarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-003 — Conservar información histórica después de eliminar un Producto")
    void cpHu18003_conservarInformacionHistorica() {
    }
}
