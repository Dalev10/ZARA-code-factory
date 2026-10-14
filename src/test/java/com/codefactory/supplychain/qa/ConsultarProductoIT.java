package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
class ConsultarProductoIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Consultar Producto existente, información de identificación y situación")
    void cpHu16001_consultarProductoExistente() {
    }

    @Test
    @DisplayName("CP-002 — Buscar Productos con los criterios definidos para la operación")
    void cpHu16002_buscarProductosConCriterios() {
    }

    @Test
    @DisplayName("CP-003 — Restringir la consulta de Productos según las responsabilidades del usuario")
    void cpHu16003_restringirConsultaSegunResponsabilidades() {
    }

    @Test
    @DisplayName("CP-004 — Consultar información logística asociada a un Producto")
    void cpHu16004_consultarInformacionLogisticaAsociada() {
    }
}
