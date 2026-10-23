package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.config.ClienteApi;
import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class PasosIdentidad {

    private static final String DESCRIPCION_ROL = "Rol de prueba para Sprint 1";

    @Autowired
    private ClienteApi api;

    @Autowired
    private ContextoPrueba contexto;

    @Cuando("registro un usuario válido con nombre {string} y contraseña {string}")
    public void registrarUsuarioValido(String nombreCompleto, String password) {
        String email = generarCorreo("hu01-001");
        contexto.texto("usuarioEmail", email);
        api.post("/usuarios", Map.of(
                "email", email,
                "nombreCompleto", nombreCompleto,
                "password", password));
        guardarUsuarioDeLaRespuesta();
    }

    @Cuando("creo un rol exclusivo y se lo asigno al usuario registrado")
    public void crearRolYAsignarlo() {
        String rolId = crearRol(ClienteApi.unico("hu01-001-rol"));
        asignarRol(contexto.id("usuarioId"), rolId);
        contexto.id("rolId", rolId);
    }

    @Cuando("consulto los roles del usuario registrado")
    public void consultarRolesDelUsuario() {
        api.get("/usuarios/" + contexto.id("usuarioId") + "/roles");
    }

    @Entonces("los roles del usuario incluyen el rol asignado")
    public void losRolesIncluyenElRolAsignado() {
        contexto.getRespuesta().then().body("id", hasItem(contexto.id("rolId")));
    }

    @Cuando("consulto el usuario registrado")
    public void consultarUsuarioRegistrado() {
        api.get("/usuarios/" + contexto.id("usuarioId"));
    }

    @Entonces("el campo {string} es el correo del usuario registrado")
    public void elCampoEsElCorreoDelUsuario(String ruta) {
        contexto.getRespuesta().then().body(ruta, equalTo(contexto.texto("usuarioEmail")));
    }

    @Cuando("registro un usuario sin nombre completo con un correo único y contraseña {string}")
    public void registrarUsuarioSinNombre(String password) {
        String email = generarCorreo("hu01-002");
        contexto.texto("usuarioEmail", email);
        api.post("/usuarios", Map.of(
                "email", email,
                "nombreCompleto", "",
                "password", password));
    }

    @Dado("que existe un usuario registrado con un correo único")
    public void usuarioRegistradoConCorreoUnico() {
        crearUsuario("QA Usuario HU01-003", "ClaveSegura2026!", "hu01-003");
    }

    @Cuando("registro otro usuario con el mismo correo")
    public void registrarUsuarioDuplicado() {
        api.post("/usuarios", Map.of(
                "email", contexto.texto("usuarioEmail"),
                "nombreCompleto", "Otro Nombre",
                "password", "OtraClaveSegura2026!"));
    }

    @Cuando("registro un usuario con nombre {string} y contraseña {string}")
    public void registrarUsuarioConDatos(String nombreCompleto, String password) {
        String email = generarCorreo("hu01-004");
        contexto.texto("usuarioEmail", email);
        api.post("/usuarios", Map.of(
                "email", email,
                "nombreCompleto", nombreCompleto,
                "password", password));
    }

    @Dado("que existe un usuario con el scope {string} y la contraseña {string}")
    public void usuarioConScope(String codigoScope, String password) {
        String rolId = crearRol(ClienteApi.unico("hu02-rol"));
        asignarScope(rolId, idDeScope(codigoScope));
        crearUsuario("Usuario QA hu02", password, "hu02");
        asignarRol(contexto.id("usuarioId"), rolId);
    }

    @Cuando("inicio sesión con el correo del usuario de prueba y la contraseña {string}")
    public void iniciarSesionConUsuarioDePrueba(String password) {
        Response respuesta = api.postSinAuth("/auth/login", Map.of(
                "email", contexto.texto("usuarioEmail"),
                "password", password));
        if (respuesta.statusCode() == 200) {
            contexto.setToken(respuesta.path("accessToken"));
        }
    }

    @Cuando("inicio sesión con un correo no registrado y la contraseña {string}")
    public void iniciarSesionConCorreoNoRegistrado(String password) {
        api.postSinAuth("/auth/login", Map.of(
                "email", generarCorreo("hu02-003-inexistente"),
                "password", password));
    }

    @Cuando("consulto los centros de distribución con la sesión iniciada")
    public void consultarCentrosDeDistribucion() {
        api.get("/centros-distribucion");
    }

    @Cuando("con ese usuario intento registrar una tienda")
    public void intentarRegistrarTiendaConUsuario() {
        api.post("/tiendas", Map.of(
                "nombre", "Tienda No Autorizada QA",
                "ubicacion", "No debería crearse"));
    }

    @Cuando("envío una tienda con un JSON mal formado")
    public void enviarTiendaConJsonMalFormado() {
        api.post("/tiendas", "{\"nombre\": \"Tienda rota\", ");
    }

    @Cuando("consulto las ventas sin iniciar sesión")
    public void consultarVentasSinSesion() {
        contexto.setToken(null);
        api.get("/ventas");
    }

    @Entonces("la cabecera {string} contiene {string}")
    public void laCabeceraContiene(String cabecera, String valor) {
        contexto.getRespuesta().then().header(cabecera, containsString(valor));
    }

    @Cuando("consulto una tienda con un identificador inexistente")
    public void consultarTiendaInexistente() {
        api.get("/tiendas/" + UUID.randomUUID());
    }

    @Cuando("intento registrar una bodega de tienda con datos vacíos")
    public void intentarRegistrarBodegaConDatosVacios() {
        api.post("/bodegas-tienda", Map.of());
    }

    @Cuando("consulto una tienda con un identificador con formato inválido")
    public void consultarTiendaConIdInvalido() {
        api.get("/tiendas/no-es-un-uuid");
    }

    @Cuando("el vendedor consulta los umbrales de reposición")
    public void vendedorConsultaLosUmbrales() {
        api.getConToken(contexto.texto("usuarioToken"), "/umbrales-reposicion");
    }

    @Cuando("consulto la última tienda creada")
    public void consultarUltimaTiendaCreada() {
        api.get("/tiendas/" + contexto.id("tiendaId"));
    }

    @Dado("que existe un vendedor con la contraseña {string}")
    public void vendedorConPassword(String password) {
        crearUsuarioConRol("VENDEDOR", password, "vendedor");
    }

    @Dado("que existe un vendedor autenticado con la contraseña {string}")
    public void vendedorAutenticado(String password) {
        crearUsuarioConRol("VENDEDOR", password, "vendedor");
        autenticarUsuarioDePrueba(password);
    }

    @Cuando("el vendedor consulta las ventas")
    public void vendedorConsultaVentas() {
        api.getConToken(contexto.texto("usuarioToken"), "/ventas");
    }

    @Cuando("el administrador desactiva al vendedor")
    public void administradorDesactivaVendedor() {
        api.put("/usuarios/" + contexto.id("usuarioId") + "/desactivar", Map.of());
    }

    @Cuando("el administrador activa al vendedor")
    public void administradorActivaVendedor() {
        api.put("/usuarios/" + contexto.id("usuarioId") + "/activar", Map.of());
    }

    @Cuando("el vendedor consulta las ventas con su token vigente")
    public void vendedorConsultaVentasConTokenVigente() {
        api.getConToken(contexto.texto("usuarioToken"), "/ventas");
    }

    @Cuando("el vendedor intenta iniciar sesión con su contraseña {string}")
    public void vendedorIntentaIniciarSesion(String password) {
        api.postSinAuth("/auth/login", Map.of(
                "email", contexto.texto("usuarioEmail"),
                "password", password));
    }

    @Cuando("el vendedor inicia sesión de nuevo con su contraseña {string}")
    public void vendedorIniciaSesionDeNuevo(String password) {
        Response respuesta = api.postSinAuth("/auth/login", Map.of(
                "email", contexto.texto("usuarioEmail"),
                "password", password));
        if (respuesta.statusCode() == 200) {
            contexto.setToken(respuesta.path("accessToken"));
        }
    }

    @Cuando("consulto las ventas con la nueva sesión")
    public void consultarVentasConNuevaSesion() {
        api.get("/ventas");
    }

    @Dado("que existe un gestor de usuarios autenticado con la contraseña {string}")
    public void gestorAutenticado(String password) {
        String rolId = crearRol(ClienteApi.unico("hu31-gestor-rol"));
        asignarScope(rolId, idDeScope("usuarios:administrar"));
        crearUsuario("Usuario QA gestor", password, "gestor");
        asignarRol(contexto.id("usuarioId"), rolId);
        autenticarUsuarioDePrueba(password);
    }

    @Cuando("el gestor intenta desactivarse a sí mismo")
    public void gestorIntentaDesactivarse() {
        String tokenAdmin = contexto.getToken();
        contexto.setToken(contexto.texto("usuarioToken"));
        try {
            api.put("/usuarios/" + contexto.id("usuarioId") + "/desactivar", Map.of());
        } finally {
            contexto.setToken(tokenAdmin);
        }
    }

    @Dado("que conozco el identificador del único administrador activo")
    public void conocerIdentificadorDelAdministrador() {
        Response respuesta = api.get("/usuarios");
        exigir(respuesta, 200, "consultar los usuarios");
        String administradorId = respuesta.path(
                "find { it.email == '" + ClienteApi.ADMIN_EMAIL + "' }.id");
        if (administradorId == null) {
            throw new IllegalStateException("No se encontró el administrador sembrado");
        }
        contexto.id("administradorId", administradorId);
    }

    @Cuando("el gestor intenta desactivar al administrador")
    public void gestorIntentaDesactivarAdministrador() {
        String tokenAdmin = contexto.getToken();
        contexto.setToken(contexto.texto("usuarioToken"));
        try {
            api.put("/usuarios/" + contexto.id("administradorId") + "/desactivar", Map.of());
        } finally {
            contexto.setToken(tokenAdmin);
        }
    }

    @Cuando("el gestor consulta al administrador")
    public void gestorConsultaAdministrador() {
        api.getConToken(contexto.texto("usuarioToken"),
                "/usuarios/" + contexto.id("administradorId"));
    }

    @Cuando("el administrador consulta al gestor")
    public void administradorConsultaGestor() {
        api.get("/usuarios/" + contexto.id("usuarioId"));
    }

    @Cuando("el gestor asegura que el administrador quede activo")
    public void gestorReactivaAdministrador() {
        String tokenAdmin = contexto.getToken();
        contexto.setToken(contexto.texto("usuarioToken"));
        try {
            api.put("/usuarios/" + contexto.id("administradorId") + "/activar", Map.of());
        } finally {
            contexto.setToken(tokenAdmin);
        }
    }

    @Cuando("intento desactivar a un usuario inexistente")
    public void desactivarUsuarioInexistente() {
        api.put("/usuarios/" + UUID.randomUUID() + "/desactivar", Map.of());
    }

    @Entonces("la operación responde {int} o {int}")
    public void laRespuestaEsUnoDe(int primero, int segundo) {
        contexto.getRespuesta().then().statusCode(anyOf(is(primero), is(segundo)));
    }

    @Dado("que existe una tienda con una variante disponible")
    public void tiendaConVarianteDisponible() {
        Response tienda = api.post("/tiendas", Map.of(
                "nombre", ClienteApi.unico("Tienda HU32"),
                "ubicacion", "Medellín, Antioquia"));
        exigir(tienda, 201, "registrar la tienda");
        contexto.id("tiendaId", tienda.path("id"));
        contexto.id("almacenId", tienda.path("almacenId"));

        Response categoria = api.post("/categorias", Map.of(
                "nombre", ClienteApi.unico("Cat HU32")));
        exigir(categoria, 201, "registrar la categoría");

        Response template = api.post("/templates", Map.of(
                "nombre", ClienteApi.unico("Template HU32"),
                "temporada", "OI-2026",
                "proveedor", "Proveedor QA",
                "precioBase", 89900,
                "categoriaId", categoria.path("id")));
        exigir(template, 201, "registrar el template");

        String sku = ClienteApi.unico("SKU-HU32").toUpperCase();
        Response variante = api.post("/variantes", Map.of(
                "sku", sku,
                "talla", "M",
                "color", "Negro",
                "templateId", template.path("id")));
        exigir(variante, 201, "registrar la variante");
        contexto.id("varianteId", variante.path("id"));
        contexto.texto("varianteSku", sku);
    }

    @Dado("que la tienda tiene {int} unidades disponibles de la variante")
    public void tiendaTieneUnidadesDisponibles(int cantidad) {
        Response respuesta = api.post(
                "/nodos/" + contexto.id("almacenId") + "/inventario/carga-inicial",
                Map.of("items", List.of(Map.of(
                        "sku", contexto.texto("varianteSku"),
                        "cantidad", cantidad))));
        exigir(respuesta, 201, "cargar el inventario de la tienda");
    }

    @Cuando("el administrador asigna la tienda al vendedor")
    public void administradorAsignaTienda() {
        Response respuesta = api.post(
                "/usuarios/" + contexto.id("usuarioId") + "/tiendas",
                Map.of("tiendaId", contexto.id("tiendaId")));
        exigir(respuesta, 204, "asignar la tienda al vendedor");
    }

    @Cuando("consulto las tiendas asignadas al vendedor")
    public void consultarTiendasAsignadas() {
        api.get("/usuarios/" + contexto.id("usuarioId") + "/tiendas");
    }

    @Entonces("las tiendas asignadas incluyen la tienda del escenario")
    public void tiendasAsignadasIncluyenLaTienda() {
        contexto.getRespuesta().then().body("id", hasItem(contexto.id("tiendaId")));
    }

    @Entonces("la tienda asignada está activa")
    public void tiendaAsignadaEstaActiva() {
        contexto.getRespuesta().then().body(
                "find { it.id == '" + contexto.id("tiendaId") + "' }.activa",
                equalTo(true));
    }

    @Cuando("el administrador quita la tienda al vendedor")
    public void administradorQuitaTienda() {
        Response respuesta = api.delete(
                "/usuarios/" + contexto.id("usuarioId") + "/tiendas/" + contexto.id("tiendaId"));
        exigir(respuesta, 204, "quitar la tienda al vendedor");
    }

    @Cuando("el administrador asigna una tienda inexistente al vendedor")
    public void administradorAsignaTiendaInexistente() {
        api.post("/usuarios/" + contexto.id("usuarioId") + "/tiendas",
                Map.of("tiendaId", UUID.randomUUID().toString()));
    }

    @Cuando("el vendedor intenta registrar una venta de una unidad en la tienda")
    public void vendedorIntentaRegistrarVenta() {
        api.postConToken(contexto.texto("usuarioToken"), "/ventas", cuerpoDeVenta(1));
    }

    @Cuando("el vendedor registra una venta de una unidad en la tienda")
    public void vendedorRegistraVenta() {
        api.postConToken(contexto.texto("usuarioToken"), "/ventas", cuerpoDeVenta(1));
    }

    @Cuando("el administrador registra una venta de una unidad en la tienda")
    public void administradorRegistraVenta() {
        api.post("/ventas", cuerpoDeVenta(1));
    }

    @Entonces("la tienda conserva {int} unidades disponibles")
    public void tiendaConservaUnidades(int cantidad) {
        Response respuesta = api.get(
                "/variantes/" + contexto.id("varianteId") + "/inventario");
        exigir(respuesta, 200, "consultar el inventario de la variante");
        List<Map<String, Object>> filas = respuesta.path(
                "findAll { it.nodoId == '" + contexto.id("almacenId") + "' }");
        int aLaMano = filas.isEmpty() ? 0
                : ((Number) filas.get(0).get("aLaMano")).intValue();
        assertThat(aLaMano).isEqualTo(cantidad);
    }

    @Cuando("consulto los scopes del rol {string}")
    public void consultarScopesDelRol(String nombreRol) {
        api.get("/roles/" + idDeRol(nombreRol) + "/scopes");
    }

    @Entonces("los scopes del rol son exactamente:")
    public void losScopesSonExactamente(DataTable tabla) {
        List<String> esperados = tabla.asList(String.class);
        List<String> codigos = contexto.getRespuesta().path("codigo");
        assertThat(codigos).containsExactlyInAnyOrderElementsOf(esperados);
    }

    @Entonces("los scopes del rol incluyen:")
    public void losScopesIncluyen(DataTable tabla) {
        List<String> esperados = tabla.asList(String.class);
        List<String> codigos = contexto.getRespuesta().path("codigo");
        assertThat(codigos).containsAll(esperados);
    }

    @Cuando("creo un rol con nombre {string} y descripción {string}")
    public void crearRolConNombre(String nombre, String descripcion) {
        api.post("/roles", Map.of("nombre", nombre, "descripcion", descripcion));
    }

    @Cuando("creo un scope con un código único y descripción {string}")
    public void crearScopeConCodigoUnico(String descripcion) {
        api.post("/scopes", Map.of(
                "codigo", ClienteApi.unico("qa:scope"),
                "descripcion", descripcion));
    }

    @Entonces("el scope queda con sensible en falso")
    public void elScopeQuedaConSensibleEnFalso() {
        contexto.getRespuesta().then().body("sensible", equalTo(false));
    }

    @Cuando("el vendedor intenta anular una venta inexistente con motivo {string}")
    public void vendedorIntentaAnularVenta(String motivo) {
        api.postConToken(contexto.texto("usuarioToken"),
                "/ventas/" + UUID.randomUUID() + "/anular",
                Map.of("motivo", motivo));
    }

    @After
    public void reactivarAdministradorSiQuedoInactivo() {
        String administradorId = contexto.id("administradorId");
        String tokenGestor = contexto.texto("usuarioToken");
        if (administradorId == null || tokenGestor == null) {
            return;
        }
        String tokenActual = contexto.getToken();
        contexto.setToken(tokenGestor);
        try {
            api.put("/usuarios/" + administradorId + "/activar", Map.of());
        } finally {
            contexto.setToken(tokenActual);
        }
    }

    private String generarCorreo(String prefijo) {
        return ClienteApi.unico(prefijo) + "@supplychain-test.local";
    }

    private void guardarUsuarioDeLaRespuesta() {
        Response respuesta = contexto.getRespuesta();
        if (respuesta.statusCode() == 201) {
            contexto.id("usuarioId", respuesta.path("id"));
        }
    }

    private String crearUsuario(String nombreCompleto, String password, String prefijo) {
        String email = generarCorreo(prefijo);
        contexto.texto("usuarioEmail", email);
        Response respuesta = api.post("/usuarios", Map.of(
                "email", email,
                "nombreCompleto", nombreCompleto,
                "password", password));
        exigir(respuesta, 201, "registrar el usuario " + email);
        contexto.id("usuarioId", respuesta.path("id"));
        return email;
    }

    private void crearUsuarioConRol(String nombreRol, String password, String prefijo) {
        crearUsuario("Usuario QA " + prefijo, password, prefijo);
        asignarRol(contexto.id("usuarioId"), idDeRol(nombreRol));
    }

    private void autenticarUsuarioDePrueba(String password) {
        String token = api.iniciarSesion(contexto.texto("usuarioEmail"), password);
        contexto.texto("usuarioToken", token);
    }

    private String crearRol(String nombre) {
        Response respuesta = api.post("/roles", Map.of(
                "nombre", nombre,
                "descripcion", DESCRIPCION_ROL));
        exigir(respuesta, 201, "registrar el rol " + nombre);
        return respuesta.path("id");
    }

    private String idDeRol(String nombre) {
        Response respuesta = api.get("/roles");
        exigir(respuesta, 200, "consultar los roles");
        String id = respuesta.path("find { it.nombre == '" + nombre + "' }.id");
        if (id == null) {
            throw new IllegalStateException("No se encontró el rol " + nombre);
        }
        return id;
    }

    private String idDeScope(String codigo) {
        Response respuesta = api.get("/scopes");
        exigir(respuesta, 200, "consultar los scopes");
        String id = respuesta.path("find { it.codigo == '" + codigo + "' }.id");
        if (id == null) {
            throw new IllegalStateException("No se encontró el scope " + codigo);
        }
        return id;
    }

    private void asignarScope(String rolId, String scopeId) {
        Response respuesta = api.post("/roles/" + rolId + "/scopes",
                Map.of("scopeId", scopeId));
        exigir(respuesta, 204, "asignar el scope al rol");
    }

    private void asignarRol(String usuarioId, String rolId) {
        Response respuesta = api.post("/usuarios/" + usuarioId + "/roles",
                Map.of("rolId", rolId));
        exigir(respuesta, 204, "asignar el rol al usuario");
    }

    private String cuerpoDeVenta(int cantidad) {
        return """
                {"transaccionId": "%s", "tiendaId": "%s", "canal": "TIENDA", "forzarSinStock": false,
                 "items": [{"sku": "%s", "cantidad": %d, "precioUnitario": 89900.00}]}
                """.formatted(UUID.randomUUID(), contexto.id("tiendaId"),
                contexto.texto("varianteSku"), cantidad);
    }

    private void exigir(Response respuesta, int esperado, String accion) {
        if (respuesta.statusCode() != esperado) {
            throw new IllegalStateException("No se pudo " + accion + ": "
                    + respuesta.statusCode() + " " + respuesta.asString());
        }
    }
}
