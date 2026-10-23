# Pruebas de aceptación (BDD)

Cada caso de aceptación (`CP-HUxx-yyy`) es un escenario Gherkin ejecutado con Cucumber
sobre la API real (RestAssured + PostgreSQL en Testcontainers). Los pasos están
implementados en Java; no hay una capa que "ejecute un test" por debajo.

```
qa/
├── features/                   Escenarios .feature (se copian al classpath desde aquí)
│   ├── sprint1/
│   └── sprint2/
├── steps/                      Step definitions y hooks de Cucumber
│   ├── PasosComunes.java       Autenticación, códigos y aserciones de contrato
│   ├── PasosRecursos.java      Creación de tiendas, CDs, categorías, templates y variantes
│   ├── PasosTiendas.java       Registro/consulta/modificación/eliminación de tiendas
│   ├── PasosInventario.java    Bodegas, inventario, carga inicial, ajustes, kardex
│   ├── PasosCatalogo.java      Productos, templates y categorías
│   ├── PasosVentas.java        Ventas, solicitudes no satisfechas y ventanas de quiebre
│   ├── PasosReposicion.java    Umbrales, traspasos, evaluación masiva y prioridad de CDs
│   └── GanchosComunes.java     Omite los escenarios etiquetados con @omitido
├── config/                     Infraestructura de ejecución
│   ├── CucumberIT.java         Runner de JUnit Platform + Cucumber
│   ├── ConfiguracionAceptacion.java  Spring Boot + Testcontainers
│   └── ClienteApi.java         Cliente HTTP y sesión del escenario
└── soporte/                    Estado del escenario y datos de apoyo por módulo
```

## Ejecutar

```
./mvnw clean -P casos-qa verify                          # todos + reporte HTML
./mvnw clean -P casos-qa verify -Dcucumber.filter.tags="@sprint2"
./mvnw clean -P casos-qa verify -Dcucumber.filter.tags="@inventario"
./mvnw clean -P casos-qa verify -Dcucumber.filter.tags="@CP-HU40-001"
./mvnw clean -P casos-qa verify -Dcucumber.filter.tags="@negativo"
```

Etiquetas: `@sprint1`/`@sprint2`, `@identidad`/`@inventario`/`@catalogo`/`@ventas`/`@reposicion`
/`@transversal`, `@CP-HUxx-yyy`, `@positivo`/`@negativo`, `@funcional`/`@no-funcional` y
`@flujo-principal`/`@ruta-negativa`/`@caso-borde`/`@seguridad`/`@fiabilidad`/`@interoperabilidad`.

Los escenarios `@omitido` (funcionalidad aún no implementada) se reportan como omitidos.

El reporte queda en `target/reports/casos-prueba.html`. El perfil usa
`testFailureIgnore=true`, así que revisar el resumen del reporte además del código de salida.

Usar siempre `clean`: sin él, el reporte mezcla resultados viejos de `target/failsafe-reports`.

## Agregar un escenario

1. Escribe el escenario en el `.feature` del sprint que corresponda.
2. Etiquétalo con su `@CP-HUxx-yyy` y los tags de tipo/ámbito/módulo.
3. Implementa los pasos que falten en `steps/Pasos<Modulo>.java`. Reutiliza los pasos
   comunes y los de `PasosRecursos` antes de crear unos nuevos.
4. Ejecuta solo ese caso con `-Dcucumber.filter.tags="@CP-HUxx-yyy"`.
