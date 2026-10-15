package com.codefactory.supplychain;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * HU-28: falla el build si un módulo usa un tipo interno de otro o si aparece un
 * ciclo entre módulos (la regla "no negociable" del README, verificada en vez de
 * confiada a la disciplina del equipo).
 */
class ModularidadTest {

    @Test
    void losModulosRespetanSusFronteras() {
        ApplicationModules.of(SupplyChainMvpApplication.class).verify();
    }
}
