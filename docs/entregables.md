# Entregables de arquitectura · Sprint 2

Índice de los seis entregables de arquitectura de software. Cada uno vive junto al código
que describe, así que cambia en el mismo pull request que el sistema. La versión para
entregar, con portada y capturas, es el documento Word del equipo; este archivo es su
fuente en el repositorio.

| # | Entregable | Dónde está | En una línea |
|---|---|---|---|
| 1 | Diagrama de despliegue | [`arquitectura/uml-despliegue.puml`](arquitectura/uml-despliegue.puml) | Render (una instancia), Supabase con TLS, Have I Been Pwned y la entrega continua real |
| 2 | APIs priorizadas | [`arquitectura/apis-priorizadas.md`](arquitectura/apis-priorizadas.md) | 89 operaciones en cuatro niveles por criticidad operativa |
| 3 | APIs REST | [`arquitectura/apis-priorizadas.md`](arquitectura/apis-priorizadas.md#las-tres-apis-rest-principales) y Swagger | 89 operaciones en 21 controladores; principales: Autenticación, Ventas e Inventario |
| 4 | CI/CD con GitHub Actions | [`ci-cd.md`](ci-cd.md) y [`../.github/workflows/ci.yml`](../.github/workflows/ci.yml) | Verificación, Quality Gate de SonarCloud y despliegue en Render solo desde `main` |
| 5 | OWASP Top 10:2025 | [`arquitectura/owasp-top10-2025.md`](arquitectura/owasp-top10-2025.md) | Controles con evidencia y 13 hallazgos; sin críticos ni altos abiertos |
| 6 | Documentación de las APIs | [Swagger UI en producción](https://supplychain-api-dzmc.onrender.com/swagger-ui.html) | 89 de 89 operaciones documentadas; `OpenApiConfigTest` lo exige |

## Enlaces

- Producción: <https://supplychain-api-dzmc.onrender.com/swagger-ui.html>
- Contrato OpenAPI 3.1: <https://supplychain-api-dzmc.onrender.com/v3/api-docs>
- Calidad del código: [SonarCloud](https://sonarcloud.io/project/overview?id=Dalev10_ZARA-code-factory)

## Ver el diagrama

El diagrama es PlantUML. Se renderiza con la extensión *PlantUML* de VS Code (`Alt+D`), que
necesita Java y Graphviz, o pegando el archivo en el servidor oficial de PlantUML.
