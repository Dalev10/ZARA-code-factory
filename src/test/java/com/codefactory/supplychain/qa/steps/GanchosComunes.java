package com.codefactory.supplychain.qa.steps;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.opentest4j.TestAbortedException;

public class GanchosComunes {

    @Before
    public void omitirEscenariosPendientes(Scenario escenario) {
        if (escenario.getSourceTagNames().contains("@omitido")) {
            throw new TestAbortedException("Escenario pendiente de implementación: " + escenario.getName());
        }
    }
}
