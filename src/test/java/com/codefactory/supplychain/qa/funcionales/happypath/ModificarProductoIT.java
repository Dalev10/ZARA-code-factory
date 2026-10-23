package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
@Tag("funcional")
@Tag("happy-path")
class ModificarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Modificar campos permitidos de un Producto")
    void cpHu17001_modificarCamposPermitidos() {
    }
}
