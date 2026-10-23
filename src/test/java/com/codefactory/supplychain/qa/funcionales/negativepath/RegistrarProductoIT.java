package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver Javadoc de la clase).")
@Tag("funcional")
@Tag("negative-path")
class RegistrarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-002 — Rechazar registro de Producto con información obligatoria incompleta")
    void cpHu15002_rechazarInformacionIncompleta() {
    }

    @Test
    @DisplayName("CP-003 — Rechazar registro de Producto duplicado")
    void cpHu15003_rechazarProductoDuplicado() {
    }
}
