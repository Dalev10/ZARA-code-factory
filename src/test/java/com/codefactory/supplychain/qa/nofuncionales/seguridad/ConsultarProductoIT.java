package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
@Tag("no-funcional")
@Tag("seguridad")
class ConsultarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-003 — Restringir la consulta de Productos según las responsabilidades del usuario")
    void cpHu16003_restringirConsultaSegunResponsabilidades() {
    }
}
