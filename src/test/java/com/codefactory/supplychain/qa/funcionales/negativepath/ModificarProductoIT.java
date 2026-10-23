package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
@Tag("funcional")
@Tag("negative-path")
class ModificarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-002 — Rechazar modificación inválida de un Producto")
    void cpHu17002_rechazarModificacionInvalida() {
    }
}
