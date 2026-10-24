package com.codefactory.supplychain.qa.soporte;

import com.codefactory.supplychain.qa.config.ClienteApi;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Datos y operaciones de apoyo para los escenarios de reposición:
 * umbrales, traspasos automáticos y manuales, evaluación masiva y prioridad de CDs.
 * Todos los ids se guardan en {@link ContextoPrueba} usando el alias del recurso.
 */
@Component
@ScenarioScope
public class ReposicionSoporte {

    private final ClienteApi api;
    private final ContextoPrueba contexto;

    public ReposicionSoporte(ClienteApi api, ContextoPrueba contexto) {
        this.api = api;
        this.contexto = contexto;
    }

    public record Nodos(String tiendaId, String almacenId, String bodegaId) {
    }

    public record Variante(String id, String sku, String categoriaId) {
    }

    public record Usuario(String id, String email, String token) {
    }

    public String tokenAdmin() {
        return contexto.getToken();
    }

    public String idTienda(String alias) {
        return contexto.id("tienda:" + alias);
    }

    public String idCentro(String alias) {
        return contexto.id("cd:" + alias);
    }

    public String idVariante(String alias) {
        return contexto.id("variante:" + alias);
    }

    public String idCategoria(String alias) {
        return contexto.id("categoria:" + alias);
    }

    public Nodos nodos(String tiendaAlias) {
        String tiendaId = idTienda(tiendaAlias);
        String clave = "reposicion:tienda:" + tiendaAlias;
        String almacenId = contexto.id(clave + ":almacen");
        if (almacenId == null) {
            Response respuesta = api.get("/tiendas/" + tiendaId);
            respuesta.then().statusCode(200);
            almacenId = respuesta.path("almacenId");
            contexto.id(clave + ":almacen", almacenId);
            contexto.id(clave + ":bodega", respuesta.path("bodegaId"));
        }
        return new Nodos(tiendaId, almacenId, contexto.id(clave + ":bodega"));
    }

    public Variante variante(String varianteAlias) {
        String id = idVariante(varianteAlias);
        String clave = "reposicion:variante:" + varianteAlias;
        String sku = contexto.texto(clave + ":sku");
        if (sku == null) {
            Response respuesta = api.get("/variantes/" + id);
            respuesta.then().statusCode(200);
            sku = respuesta.path("sku");
            Response template = api.get("/templates/" + respuesta.path("templateId"));
            template.then().statusCode(200);
            contexto.texto(clave + ":sku", sku);
            contexto.texto(clave + ":categoria", template.path("categoriaId"));
        }
        return new Variante(id, sku, contexto.texto(clave + ":categoria"));
    }

    // ------------------------------------------------------------------ inventario

    public Response cargarEnAlmacen(String tiendaAlias, String varianteAlias, int cantidad) {
        Variante variante = variante(varianteAlias);
        Nodos nodos = nodos(tiendaAlias);
        return cargarInicial(nodos.almacenId(), variante.sku(), cantidad);
    }

    public Response cargarEnBodega(String tiendaAlias, String varianteAlias, int cantidad) {
        Variante variante = variante(varianteAlias);
        Nodos nodos = nodos(tiendaAlias);
        return cargarInicial(nodos.bodegaId(), variante.sku(), cantidad);
    }

    private Response cargarInicial(String nodoId, String sku, int cantidad) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sku", sku);
        item.put("cantidad", cantidad);
        Response respuesta = api.post("/nodos/" + nodoId + "/inventario/carga-inicial",
                Map.of("items", List.of(item)));
        respuesta.then().statusCode(201);
        return respuesta;
    }

    public Response ajustarBodega(String tiendaAlias, String varianteAlias, int delta, String motivo) {
        Variante variante = variante(varianteAlias);
        Nodos nodos = nodos(tiendaAlias);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sku", variante.sku());
        cuerpo.put("delta", delta);
        cuerpo.put("motivo", motivo);
        cuerpo.put("comentario", null);
        Response respuesta = api.post("/nodos/" + nodos.bodegaId() + "/inventario/ajustes", cuerpo);
        respuesta.then().statusCode(201);
        return respuesta;
    }

    public int stock(String tiendaAlias, String varianteAlias, boolean almacen) {
        Nodos nodos = nodos(tiendaAlias);
        Variante variante = variante(varianteAlias);
        String nodoId = almacen ? nodos.almacenId() : nodos.bodegaId();
        Response respuesta = api.get("/variantes/" + variante.id() + "/inventario");
        respuesta.then().statusCode(200);
        List<Map<String, Object>> filas = respuesta.path(
                "findAll { it.nodoId == '" + nodoId + "' }");
        return filas.isEmpty() ? 0 : ((Number) filas.get(0).get("aLaMano")).intValue();
    }

    public Response vender(String token, String tiendaAlias, String varianteAlias,
                           int cantidad, boolean forzarSinStock) {
        Variante variante = variante(varianteAlias);
        Nodos nodos = nodos(tiendaAlias);
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("sku", variante.sku());
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", 89900.00);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("transaccionId", UUID.randomUUID().toString());
        cuerpo.put("tiendaId", nodos.tiendaId());
        cuerpo.put("canal", "TIENDA");
        cuerpo.put("forzarSinStock", forzarSinStock);
        cuerpo.put("items", List.of(linea));
        Response respuesta = api.postConToken(token, "/ventas", cuerpo);
        respuesta.then().statusCode(201);
        return respuesta;
    }

    // ------------------------------------------------------------------ umbrales

    public Response crearUmbralVariante(String varianteAlias, int minimo, int objetivo) {
        Response respuesta = postUmbralVariante(varianteAlias, minimo, objetivo);
        respuesta.then().statusCode(201);
        return respuesta;
    }

    public Response crearUmbralCategoria(String categoriaAlias, int minimo, int objetivo) {
        Response respuesta = postUmbralCategoria(categoriaAlias, minimo, objetivo);
        respuesta.then().statusCode(201);
        return respuesta;
    }

    public Response postUmbralVariante(String varianteAlias, int minimo, int objetivo) {
        Variante variante = variante(varianteAlias);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("varianteId", variante.id());
        cuerpo.put("cantidadMinima", minimo);
        cuerpo.put("cantidadObjetivo", objetivo);
        Response respuesta = api.post("/umbrales-reposicion", cuerpo);
        guardarUmbral("reposicion:umbral:variante:" + varianteAlias, respuesta);
        return respuesta;
    }

    public Response postUmbralCategoria(String categoriaAlias, int minimo, int objetivo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("categoriaId", idCategoria(categoriaAlias));
        cuerpo.put("cantidadMinima", minimo);
        cuerpo.put("cantidadObjetivo", objetivo);
        Response respuesta = api.post("/umbrales-reposicion", cuerpo);
        guardarUmbral("reposicion:umbral:categoria:" + categoriaAlias, respuesta);
        return respuesta;
    }

    public Response postUmbralMixto(String varianteAlias, String categoriaAlias, int minimo, int objetivo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("varianteId", idVariante(varianteAlias));
        cuerpo.put("categoriaId", idCategoria(categoriaAlias));
        cuerpo.put("cantidadMinima", minimo);
        cuerpo.put("cantidadObjetivo", objetivo);
        return api.post("/umbrales-reposicion", cuerpo);
    }

    public Response postUmbralSinAlcance(int minimo, int objetivo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("cantidadMinima", minimo);
        cuerpo.put("cantidadObjetivo", objetivo);
        return api.post("/umbrales-reposicion", cuerpo);
    }

    public Response postUmbralVarianteInexistente(int minimo, int objetivo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("varianteId", UUID.randomUUID().toString());
        cuerpo.put("cantidadMinima", minimo);
        cuerpo.put("cantidadObjetivo", objetivo);
        return api.post("/umbrales-reposicion", cuerpo);
    }

    public Response postUmbralComo(String token, String varianteAlias, int minimo, int objetivo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("varianteId", idVariante(varianteAlias));
        cuerpo.put("cantidadMinima", minimo);
        cuerpo.put("cantidadObjetivo", objetivo);
        return api.postConToken(token, "/umbrales-reposicion", cuerpo);
    }

    public Response actualizarUmbralVariante(String varianteAlias, int minimo, int objetivo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("cantidadMinima", minimo);
        cuerpo.put("cantidadObjetivo", objetivo);
        return api.put("/umbrales-reposicion/" + contexto.id("reposicion:umbral:variante:" + varianteAlias),
                cuerpo);
    }

    public Response consultarUmbralVariante(String varianteAlias) {
        return api.get("/umbrales-reposicion/"
                + contexto.id("reposicion:umbral:variante:" + varianteAlias));
    }

    public Response consultarUmbralCreado() {
        return api.get("/umbrales-reposicion/" + contexto.id("reposicion:umbral:ultimo"));
    }

    public Response eliminarUmbralVariante(String varianteAlias) {
        return api.delete("/umbrales-reposicion/"
                + contexto.id("reposicion:umbral:variante:" + varianteAlias));
    }

    private void guardarUmbral(String clave, Response respuesta) {
        if (respuesta.statusCode() == 201) {
            String id = respuesta.path("id");
            contexto.id(clave, id);
            contexto.id("reposicion:umbral:ultimo", id);
        }
    }

    // ------------------------------------------------------------------ traspasos

    public List<Map<String, Object>> traspasos(String tiendaAlias) {
        Response respuesta = api.get("/tiendas/" + idTienda(tiendaAlias) + "/traspasos-internos");
        respuesta.then().statusCode(200);
        return respuesta.path("$");
    }

    public Response consultarTraspasosComo(String token, String tiendaAlias) {
        return api.getConToken(token, "/tiendas/" + idTienda(tiendaAlias) + "/traspasos-internos");
    }

    public Response traspasoManual(String token, String tiendaAlias, String varianteAlias, int cantidad) {
        Variante variante = variante(varianteAlias);
        Nodos nodos = nodos(tiendaAlias);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("varianteId", variante.id());
        cuerpo.put("cantidad", cantidad);
        Response respuesta = api.postConToken(token,
                "/tiendas/" + nodos.tiendaId() + "/traspasos-internos", cuerpo);
        if (respuesta.statusCode() == 201) {
            int indice = siguienteIndiceTraspaso();
            contexto.id("reposicion:traspaso:" + indice + ":id", respuesta.path("id"));
            contexto.texto("reposicion:traspaso:" + indice + ":tienda", tiendaAlias);
        }
        return respuesta;
    }

    public Response traspasoManualSinVariante(String token, String tiendaAlias, int cantidad) {
        Nodos nodos = nodos(tiendaAlias);
        return api.postConToken(token,
                "/tiendas/" + nodos.tiendaId() + "/traspasos-internos",
                Map.of("cantidad", cantidad));
    }

    public Response traspasoManualVarianteInexistente(String token, String tiendaAlias, int cantidad) {
        Nodos nodos = nodos(tiendaAlias);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("varianteId", UUID.randomUUID().toString());
        cuerpo.put("cantidad", cantidad);
        return api.postConToken(token,
                "/tiendas/" + nodos.tiendaId() + "/traspasos-internos", cuerpo);
    }

    public Response consultarTraspaso(int indice) {
        return api.get("/traspasos-internos/" + idTraspaso(indice));
    }

    public Response consultarTraspasoInexistente() {
        return api.get("/traspasos-internos/" + UUID.randomUUID());
    }

    public String idTraspaso(int indice) {
        return contexto.id("reposicion:traspaso:" + indice + ":id");
    }

    public String tiendaDelTraspaso(int indice) {
        return contexto.texto("reposicion:traspaso:" + indice + ":tienda");
    }

    private int siguienteIndiceTraspaso() {
        String contador = contexto.texto("reposicion:traspaso:contador");
        int indice = contador == null ? 1 : Integer.parseInt(contador) + 1;
        contexto.texto("reposicion:traspaso:contador", String.valueOf(indice));
        return indice;
    }

    public Response kardex(String tiendaAlias, boolean almacen, String tipo) {
        Nodos nodos = nodos(tiendaAlias);
        String nodoId = almacen ? nodos.almacenId() : nodos.bodegaId();
        return api.get("/nodos/" + nodoId + "/inventario/movimientos", Map.of("tipo", tipo));
    }

    public Map<String, Object> ventana(String tiendaAlias, String varianteAlias) {
        Response respuesta = api.get("/ventanas-quiebre", Map.of(
                "tiendaId", idTienda(tiendaAlias),
                "varianteId", idVariante(varianteAlias)));
        respuesta.then().statusCode(200);
        List<Map<String, Object>> content = respuesta.path("content");
        return content.isEmpty() ? null : content.get(0);
    }

    // ------------------------------------------------------------------ evaluación masiva

    public Response evaluarTienda(String tiendaAlias) {
        return api.post("/reposicion-interna/tiendas/" + idTienda(tiendaAlias) + "/evaluar", Map.of());
    }

    public Response evaluarTodas() {
        return api.post("/reposicion-interna/evaluar", Map.of());
    }

    public Response evaluarTiendaInexistente() {
        return api.post("/reposicion-interna/tiendas/" + UUID.randomUUID() + "/evaluar", Map.of());
    }

    public Response evaluarTodasComo(String token) {
        return api.postConToken(token, "/reposicion-interna/evaluar", Map.of());
    }

    public Response desactivarTienda(String tiendaAlias) {
        return api.delete("/tiendas/" + idTienda(tiendaAlias));
    }

    // ------------------------------------------------------------------ prioridad de CDs

    public Response definirPrioridad(String tiendaAlias, List<String> cdAliases) {
        List<String> ids = cdAliases.stream().map(this::idCentro).toList();
        return definirPrioridadConIds(idTienda(tiendaAlias), ids);
    }

    public Response definirPrioridadConIds(String tiendaId, List<String> cdIds) {
        return api.put("/tiendas/" + tiendaId + "/prioridad-cd", Map.of("cdIds", cdIds));
    }

    public Response definirPrioridadSinLista(String tiendaAlias) {
        return api.put("/tiendas/" + idTienda(tiendaAlias) + "/prioridad-cd", Map.of());
    }

    public Response definirPrioridadComo(String token, String tiendaAlias, List<String> cdAliases) {
        String tokenAdmin = contexto.getToken();
        contexto.setToken(token);
        try {
            return definirPrioridad(tiendaAlias, cdAliases);
        } finally {
            contexto.setToken(tokenAdmin);
        }
    }

    public Response consultarPrioridad(String tiendaAlias) {
        return api.get("/tiendas/" + idTienda(tiendaAlias) + "/prioridad-cd");
    }

    public List<String> idsPrioridad() {
        List<Map<String, Object>> filas = new ArrayList<>(contexto.getRespuesta().path("$"));
        filas.sort(Comparator.comparingInt(fila ->
                ((Number) fila.get("ordenPrioridad")).intValue()));
        return filas.stream().map(fila -> String.valueOf(fila.get("cdId"))).toList();
    }

    // ------------------------------------------------------------------ usuarios

    public Usuario crearSupervisor(String alias) {
        return crearUsuarioConRol(alias, "SUPERVISOR_TIENDA", "reposicion:supervisor:");
    }

    public Usuario crearUsuarioConScope(String alias, String scope) {
        return crearUsuarioConScopes(alias, "reposicion:usuario:", scope);
    }

    public String tokenSupervisor(String alias) {
        return contexto.texto("reposicion:supervisor:" + alias + ":token");
    }

    public String tokenUsuario(String alias) {
        return contexto.texto("reposicion:usuario:" + alias + ":token");
    }

    private Usuario crearUsuarioConRol(String alias, String rol, String prefijoClave) {
        String rolId = idDeRol(rol);
        Usuario usuario = crearUsuario(alias, rolId);
        contexto.texto(prefijoClave + alias + ":token", usuario.token());
        contexto.id(prefijoClave + alias + ":id", usuario.id());
        return usuario;
    }

    private Usuario crearUsuarioConScopes(String alias, String prefijoClave, String... scopes) {
        Response rol = api.post("/roles", Map.of(
                "nombre", ClienteApi.unico(alias + "-rol"),
                "descripcion", "Rol de prueba de reposición"));
        rol.then().statusCode(201);
        String rolId = rol.path("id");
        for (String scope : scopes) {
            Response consulta = api.get("/scopes");
            consulta.then().statusCode(200);
            String scopeId = consulta.path("find { it.codigo == '" + scope + "' }.id");
            if (scopeId == null) {
                throw new IllegalStateException("No existe el scope " + scope);
            }
            api.post("/roles/" + rolId + "/scopes", Map.of("scopeId", scopeId))
                    .then().statusCode(204);
        }
        Usuario usuario = crearUsuario(alias, rolId);
        contexto.texto(prefijoClave + alias + ":token", usuario.token());
        contexto.id(prefijoClave + alias + ":id", usuario.id());
        return usuario;
    }

    private Usuario crearUsuario(String alias, String rolId) {
        String email = ClienteApi.unico(alias) + "@supplychain-test.local";
        Response respuesta = api.post("/usuarios", Map.of(
                "email", email,
                "nombreCompleto", "Usuario QA " + alias,
                "password", ClienteApi.PASSWORD_QA));
        respuesta.then().statusCode(201);
        String usuarioId = respuesta.path("id");
        api.post("/usuarios/" + usuarioId + "/roles", Map.of("rolId", rolId))
                .then().statusCode(204);
        String token = api.iniciarSesion(email, ClienteApi.PASSWORD_QA);
        return new Usuario(usuarioId, email, token);
    }

    private String idDeRol(String nombre) {
        Response respuesta = api.get("/roles");
        respuesta.then().statusCode(200);
        String id = respuesta.path("find { it.nombre == '" + nombre + "' }.id");
        if (id == null) {
            throw new IllegalStateException("No existe el rol " + nombre);
        }
        return id;
    }

    public String idAdministrador() {
        Response respuesta = api.get("/usuarios");
        respuesta.then().statusCode(200);
        String id = respuesta.path("find { it.email == '" + ClienteApi.ADMIN_EMAIL + "' }.id");
        if (id == null) {
            throw new IllegalStateException("No se encontró el administrador sembrado");
        }
        return id;
    }
}
