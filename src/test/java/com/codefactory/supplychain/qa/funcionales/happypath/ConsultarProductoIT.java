package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Disabled("No ejecutable: el backend no implementa la entidad Producto (ver RegistrarProductoIT).")
@Tag("funcional")
@Tag("happy-path")
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
    @DisplayName("CP-004 — Consultar información logística asociada a un Producto")
    void cpHu16004_consultarInformacionLogisticaAsociada() {
    }
}
