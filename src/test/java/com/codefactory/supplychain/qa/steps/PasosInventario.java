package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.config.ClienteApi;
import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import com.codefactory.supplychain.qa.soporte.InventarioSoporte;
import com.codefactory.supplychain.qa.soporte.InventarioSoporte.Usuario;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

public class PasosInventario {

    @Autowired
    private ClienteApi api;

    @Autowired
    private ContextoPrueba contexto;

    @Autowired
    private InventarioSoporte soporte;

    private int ventasConfirmadas;
    private boolean ventasConCodigoValido;
    private boolean reintentosConCodigoValido;
    private int codigoFinalReintento;

    // ------------------------------------------------------------------ datos de apoyo

    @Dado("que existe una tienda con almacén y bodega llamada {string} ubicada en {string}")
    public void existeTiendaConNodos(String alias, String ubicacion) {
        soporte.crearTienda(alias, ubicacion);
    }

    @Dado("que existe un centro de distribución de inventario llamado {string} ubicado en {string}")
    public void existeCentroDeDistribucion(String alias, String ubicacion) {
        soporte.crearCentro(alias, ubicacion);
    }

    @Dado("que existe una variante de inventario llamada {string}")
    public void existeVariante(String alias) {
        soporte.crearVariante(alias);
    }

    @Dado("que la variante {string} tiene {int} unidades cargadas en el almacén de la tienda {string}")
    public void cargarEnAlmacen(String variante, int cantidad, String tienda) {
        soporte.cargar(tokenAdmin(), nodoAlmacen(tienda), skuDe(variante), cantidad);
    }

    @Dado("que la variante {string} tiene {int} unidades cargadas en la bodega de la tienda {string}")
    public void cargarEnBodega(String variante, int cantidad, String tienda) {
        soporte.cargar(tokenAdmin(), nodoBodega(tienda), skuDe(variante), cantidad);
    }

    @Dado("que la variante {string} tiene {int} unidades cargadas en el nodo del centro de distribución {string}")
    public void cargarEnCentro(String variante, int cantidad, String centro) {
        soporte.cargar(tokenAdmin(), nodoCentro(centro), skuDe(variante), cantidad);
    }

    @Dado("que la tienda {string} tiene como prioridad el centro de distribución {string}")
    public void tiendaConPrioridad(String tienda, String centro) {
        soporte.asignarPrioridad(idTienda(tienda), idCentro(centro));
    }

    @Dado("que existe un vendedor asignado a la tienda {string}")
    public void existeVendedorAsignado(String tienda) {
        Usuario vendedor = soporte.crearUsuarioConRol("vendedor-" + tienda, "VENDEDOR");
        soporte.asignarTienda(vendedor.id(), idTienda(tienda));
        contexto.texto("vendedor:token", vendedor.token());
        contexto.id("vendedor:id", vendedor.id());
    }

    @Dado("que existe un operador de centro de distribución")
    public void existeOperadorDeCentro() {
        Usuario operador = soporte.crearUsuarioConRol("operador-cd", "OPERADOR_CD");
        contexto.texto("operadorCd:token", operador.token());
        contexto.id("operadorCd:id", operador.id());
    }

    @Dado("que existe un supervisor de tienda asignado a la tienda {string}")
    public void existeSupervisorAsignado(String tienda) {
        Usuario supervisor = soporte.crearUsuarioConRol("supervisor-" + tienda, "SUPERVISOR_TIENDA");
        soporte.asignarTienda(supervisor.id(), idTienda(tienda));
        contexto.texto("supervisor:token", supervisor.token());
        contexto.id("supervisor:id", supervisor.id());
    }

    @Dado("que existe un usuario con solo el permiso {string} asignado a la tienda {string}")
    public void existeUsuarioConSoloPermiso(String permiso, String tienda) {
        Usuario usuario = soporte.crearUsuarioConScopes("usuario-" + tienda, permiso);
        soporte.asignarTienda(usuario.id(), idTienda(tienda));
        contexto.texto("usuarioSinConsulta:token", usuario.token());
        contexto.id("usuarioSinConsulta:id", usuario.id());
    }

    @Dado("que la bodega de la tienda {string} existe")
    public void bodegaDeTiendaExiste(String tienda) {
        api.get("/bodegas-tienda/tienda/" + idTienda(tienda)).then().statusCode(200);
    }

    // ------------------------------------------------------------------ registro y consulta

    @Cuando("registro una tienda llamada {string} ubicada en {string}")
    public void registrarTienda(String alias, String ubicacion) {
        soporte.crearTienda(alias, ubicacion);
    }

    @Cuando("registro un centro de distribución llamado {string} ubicado en {string}")
    public void registrarCentro(String alias, String ubicacion) {
        soporte.crearCentro(alias, ubicacion);
    }

    @Cuando("registro un centro de distribución con nombre vacío ubicado en {string}")
    public void registrarCentroSinNombre(String ubicacion) {
        api.post("/centros-distribucion", Map.of("nombre", "", "ubicacion", ubicacion));
    }

    @Cuando("registro de nuevo un centro de distribución con el nombre del último centro registrado")
    public void registrarCentroDuplicado() {
        api.post("/centros-distribucion", Map.of(
                "nombre", contexto.texto("ultimoCdNombre"),
                "ubicacion", "Otra ubicación"));
    }

    @Cuando("consulto la tienda {string}")
    public void consultarTienda(String tienda) {
        api.get("/tiendas/" + idTienda(tienda));
    }

    @Cuando("consulto el listado de tiendas")
    public void consultarListadoDeTiendas() {
        api.get("/tiendas");
    }

    @Cuando("consulto la bodega de la tienda {string}")
    public void consultarBodegaPorTienda(String tienda) {
        api.get("/bodegas-tienda/tienda/" + idTienda(tienda));
    }

    @Cuando("consulto la bodega de la tienda recién registrada")
    public void consultarBodegaDeTiendaRecienRegistrada() {
        api.get("/bodegas-tienda/tienda/" + contexto.id("ultimaTiendaId"));
    }

    @Cuando("consulto directamente la bodega de la tienda {string}")
    public void consultarBodegaDirecta(String tienda) {
        api.get("/bodegas-tienda/" + idBodega(tienda));
    }

    @Cuando("consulto la bodega de una tienda inexistente")
    public void consultarBodegaDeTiendaInexistente() {
        api.get("/bodegas-tienda/tienda/" + UUID.randomUUID());
    }

    @Cuando("consulto el centro de distribución {string}")
    public void consultarCentro(String centro) {
        api.get("/centros-distribucion/" + idCentro(centro));
    }

    @Cuando("consulto el inventario del almacén de la tienda {string}")
    public void consultarInventarioDelAlmacen(String tienda) {
        api.get("/nodos/" + nodoAlmacen(tienda) + "/inventario");
    }

    @Cuando("consulto el inventario de la bodega de la tienda {string}")
    public void consultarInventarioDeLaBodega(String tienda) {
        api.get("/nodos/" + nodoBodega(tienda) + "/inventario");
    }

    @Cuando("consulto el inventario del nodo del centro de distribución {string}")
    public void consultarInventarioDelCentro(String centro) {
        api.get("/nodos/" + nodoCentro(centro) + "/inventario");
    }

    @Cuando("consulto el inventario de la variante {string}")
    public void consultarInventarioDeLaVariante(String variante) {
        api.get("/variantes/" + idVariante(variante) + "/inventario");
    }

    @Cuando("consulto el kardex del almacén de la tienda {string}")
    public void consultarKardex(String tienda) {
        api.get("/nodos/" + nodoAlmacen(tienda) + "/inventario/movimientos");
    }

    @Cuando("consulto el kardex del almacén de la tienda {string} filtrando por tipo {string}")
    public void consultarKardexPorTipo(String tienda, String tipo) {
        api.get("/nodos/" + nodoAlmacen(tienda) + "/inventario/movimientos", Map.of("tipo", tipo));
    }

    @Cuando("consulto el kardex del almacén de la tienda {string} desde una fecha futura")
    public void consultarKardexDesdeFechaFutura(String tienda) {
        api.get("/nodos/" + nodoAlmacen(tienda) + "/inventario/movimientos",
                Map.of("desde", java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS)
                        .truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()));
    }

    @Cuando("consulto el kardex del almacén de la tienda {string} entre ayer y mañana")
    public void consultarKardexEntreAyerYManana(String tienda) {
        String pasado = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS)
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        String futuro = java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS)
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        api.get("/nodos/" + nodoAlmacen(tienda) + "/inventario/movimientos",
                Map.of("desde", pasado, "hasta", futuro));
    }

    // ------------------------------------------------------------------ modificación y eliminación

    @Cuando("modifico la tienda {string} con un nombre único y ubicación {string}")
    public void modificarTienda(String tienda, String ubicacion) {
        String nombre = ClienteApi.unico("Tienda " + tienda + " Actualizada");
        contexto.texto("tienda:" + tienda + ":nombreNuevo", nombre);
        api.put("/tiendas/" + idTienda(tienda), Map.of("nombre", nombre, "ubicacion", ubicacion));
    }

    @Cuando("modifico la tienda {string} con nombre vacío y ubicación {string}")
    public void modificarTiendaSinNombre(String tienda, String ubicacion) {
        api.put("/tiendas/" + idTienda(tienda), Map.of("nombre", "", "ubicacion", ubicacion));
    }

    @Cuando("modifico el centro de distribución {string} con un nombre único y ubicación {string}")
    public void modificarCentro(String centro, String ubicacion) {
        String nombre = ClienteApi.unico("CD " + centro + " Actualizado");
        contexto.texto("cd:" + centro + ":nombreNuevo", nombre);
        api.put("/centros-distribucion/" + idCentro(centro), Map.of("nombre", nombre, "ubicacion", ubicacion));
    }

    @Cuando("modifico el centro de distribución {string} con nombre vacío y ubicación {string}")
    public void modificarCentroSinNombre(String centro, String ubicacion) {
        api.put("/centros-distribucion/" + idCentro(centro), Map.of("nombre", "", "ubicacion", ubicacion));
    }

    @Cuando("elimino la tienda {string}")
    public void eliminarTienda(String tienda) {
        api.delete("/tiendas/" + idTienda(tienda));
    }

    @Cuando("elimino la bodega de la tienda {string}")
    public void eliminarBodega(String tienda) {
        api.delete("/bodegas-tienda/" + idBodega(tienda));
    }

    @Cuando("elimino el centro de distribución {string}")
    public void eliminarCentro(String centro) {
        api.delete("/centros-distribucion/" + idCentro(centro));
    }

    @Cuando("intento registrar una bodega de tienda para una tienda inexistente")
    public void registrarBodegaParaTiendaInexistente() {
        api.post("/bodegas-tienda", Map.of("tiendaId", UUID.randomUUID().toString()));
    }

    @Cuando("intento modificar la bodega de la tienda {string}")
    public void modificarBodega(String tienda) {
        api.put("/bodegas-tienda/" + idBodega(tienda), Map.of("tiendaId", idTienda(tienda)));
    }

    // ------------------------------------------------------------------ carga inicial

    @Cuando("cargo inicialmente en el almacén de la tienda {string} los ítems:")
    public void cargarInicialmenteEnAlmacen(String tienda, DataTable items) {
        soporte.cargaInicial(tokenAdmin(), nodoAlmacen(tienda), itemsDe(items));
    }

    @Cuando("cargo inicialmente en la bodega de la tienda {string} los ítems:")
    public void cargarInicialmenteEnBodega(String tienda, DataTable items) {
        soporte.cargaInicial(tokenAdmin(), nodoBodega(tienda), itemsDe(items));
    }

    @Cuando("cargo inicialmente en el nodo del centro de distribución {string} los ítems:")
    public void cargarInicialmenteEnCentro(String centro, DataTable items) {
        soporte.cargaInicial(tokenAdmin(), nodoCentro(centro), itemsDe(items));
    }

    @Cuando("cargo inicialmente un lote vacío en el almacén de la tienda {string}")
    public void cargarLoteVacio(String tienda) {
        soporte.cargaInicial(tokenAdmin(), nodoAlmacen(tienda), List.of());
    }

    @Cuando("cargo inicialmente un lote de más de 500 ítems en el almacén de la tienda {string}")
    public void cargarLoteExcedido(String tienda) {
        soporte.cargaInicial(tokenAdmin(), nodoAlmacen(tienda),
                InventarioSoporte.loteDeMasDe500("SKU-HU37-007"));
    }

    @Cuando("cargo inicialmente en un nodo inexistente la variante {string} con {int} unidades")
    public void cargarEnNodoInexistente(String variante, int cantidad) {
        soporte.cargaInicial(tokenAdmin(), UUID.randomUUID().toString(),
                List.of(InventarioSoporte.item(skuDe(variante), cantidad)));
    }

    @Cuando("el operador de centro de distribución intenta cargar inicialmente en el almacén de la tienda {string} la variante {string} con {int} unidades")
    public void operadorIntentaCargarEnTienda(String tienda, String variante, int cantidad) {
        soporte.cargaInicial(tokenOperador(), nodoAlmacen(tienda),
                List.of(InventarioSoporte.item(skuDe(variante), cantidad)));
    }

    @Cuando("el operador de centro de distribución carga inicialmente en el nodo del centro de distribución {string} la variante {string} con {int} unidades")
    public void operadorCargaEnCentro(String centro, String variante, int cantidad) {
        soporte.cargaInicial(tokenOperador(), nodoCentro(centro),
                List.of(InventarioSoporte.item(skuDe(variante), cantidad)));
    }

    @Cuando("el supervisor intenta cargar inicialmente en el nodo del centro de distribución {string} la variante {string} con {int} unidades")
    public void supervisorIntentaCargarEnCentro(String centro, String variante, int cantidad) {
        soporte.cargaInicial(tokenSupervisor(), nodoCentro(centro),
                List.of(InventarioSoporte.item(skuDe(variante), cantidad)));
    }

    @Cuando("el supervisor intenta cargar inicialmente en el almacén de la tienda {string} la variante {string} con {int} unidades")
    public void supervisorIntentaCargarEnTienda(String tienda, String variante, int cantidad) {
        soporte.cargaInicial(tokenSupervisor(), nodoAlmacen(tienda),
                List.of(InventarioSoporte.item(skuDe(variante), cantidad)));
    }

    @Cuando("el supervisor carga inicialmente en el almacén de la tienda {string} la variante {string} con {int} unidades")
    public void supervisorCargaEnTienda(String tienda, String variante, int cantidad) {
        soporte.cargaInicial(tokenSupervisor(), nodoAlmacen(tienda),
                List.of(InventarioSoporte.item(skuDe(variante), cantidad)));
    }

    // ------------------------------------------------------------------ ajustes

    @Cuando("ajusto la variante {string} con {int} unidades por motivo {string} en el almacén de la tienda {string}")
    public void ajustarSinComentario(String variante, int delta, String motivo, String tienda) {
        soporte.ajustar(tokenAdmin(), nodoAlmacen(tienda), skuDe(variante), delta, motivo, null);
    }

    @Cuando("ajusto la variante {string} con {int} unidades por motivo {string} y comentario {string} en el almacén de la tienda {string}")
    public void ajustarConComentario(String variante, int delta, String motivo, String comentario, String tienda) {
        soporte.ajustar(tokenAdmin(), nodoAlmacen(tienda), skuDe(variante), delta, motivo, comentario);
    }

    @Cuando("ajusto la variante {string} con {int} unidades por motivo {string} con un comentario en blanco en el almacén de la tienda {string}")
    public void ajustarConComentarioEnBlanco(String variante, int delta, String motivo, String tienda) {
        soporte.ajustar(tokenAdmin(), nodoAlmacen(tienda), skuDe(variante), delta, motivo, "   ");
    }

    @Cuando("ajusto un SKU inexistente con {int} unidades por motivo {string} en el almacén de la tienda {string}")
    public void ajustarSkuInexistente(int delta, String motivo, String tienda) {
        soporte.ajustar(tokenAdmin(), nodoAlmacen(tienda), "SKU-NO-EXISTE-HU38", delta, motivo, null);
    }

    @Cuando("el vendedor ajusta la variante {string} con {int} unidades por motivo {string} en el almacén de la tienda {string}")
    public void vendedorAjusta(String variante, int delta, String motivo, String tienda) {
        soporte.ajustar(tokenVendedor(), nodoAlmacen(tienda), skuDe(variante), delta, motivo, null);
    }

    // ------------------------------------------------------------------ ventas y concurrencia

    @Cuando("registro una venta de {int} unidades de la variante {string} en la tienda {string}")
    public void registrarVenta(int cantidad, String variante, String tienda) {
        soporte.vender(tokenAdmin(), idTienda(tienda), skuDe(variante), cantidad,
                InventarioSoporte.nuevaTransaccion()).then().statusCode(201);
    }

    @Cuando("se registran {int} ventas simultáneas de una unidad de la variante {string} en la tienda {string}")
    public void ventasSimultaneas(int numeroDeVentas, String variante, String tienda) throws Exception {
        String token = tokenAdmin();
        String tiendaId = idTienda(tienda);
        String sku = skuDe(variante);
        ExecutorService pool = Executors.newFixedThreadPool(numeroDeVentas);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < numeroDeVentas; i++) {
            tareas.add(() -> InventarioSoporte.venderDirecto(token, tiendaId, sku, 1,
                    InventarioSoporte.nuevaTransaccion()).statusCode());
        }
        ventasConfirmadas = 0;
        ventasConCodigoValido = true;
        try {
            for (Future<Integer> futuro : pool.invokeAll(tareas)) {
                int codigo = futuro.get();
                if (codigo != 201 && codigo != 409) {
                    ventasConCodigoValido = false;
                }
                if (codigo == 201) {
                    ventasConfirmadas++;
                }
            }
        } finally {
            pool.shutdown();
        }
    }

    @Cuando("se reintenta en paralelo {int} veces la misma venta de {int} unidades de la variante {string} en la tienda {string}")
    public void reintentosParalelos(int numeroDeReintentos, int cantidad, String variante, String tienda)
            throws Exception {
        String token = tokenAdmin();
        Map<String, Object> cuerpo = InventarioSoporte.cuerpoVenta(InventarioSoporte.nuevaTransaccion(),
                idTienda(tienda), skuDe(variante), cantidad);
        ExecutorService pool = Executors.newFixedThreadPool(numeroDeReintentos);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < numeroDeReintentos; i++) {
            tareas.add(() -> InventarioSoporte.postDirecto(token, "/ventas", cuerpo).statusCode());
        }
        reintentosConCodigoValido = true;
        try {
            for (Future<Integer> futuro : pool.invokeAll(tareas)) {
                int codigo = futuro.get();
                if (codigo != 200 && codigo != 201 && codigo != 409) {
                    reintentosConCodigoValido = false;
                }
            }
        } finally {
            pool.shutdown();
        }
        codigoFinalReintento = api.postConToken(token, "/ventas", cuerpo).statusCode();
    }

    // ------------------------------------------------------------------ usuarios de consulta

    @Cuando("el vendedor consulta el inventario del almacén de la tienda {string}")
    public void vendedorConsultaInventario(String tienda) {
        api.getConToken(tokenVendedor(), "/nodos/" + nodoAlmacen(tienda) + "/inventario");
    }

    @Cuando("el vendedor consulta el kardex del almacén de la tienda {string}")
    public void vendedorConsultaKardex(String tienda) {
        api.getConToken(tokenVendedor(), "/nodos/" + nodoAlmacen(tienda) + "/inventario/movimientos");
    }

    @Cuando("el vendedor consulta el inventario de la variante {string}")
    public void vendedorConsultaVariante(String variante) {
        api.getConToken(tokenVendedor(), "/variantes/" + idVariante(variante) + "/inventario");
    }

    @Cuando("el vendedor consulta el inventario del nodo del centro de distribución {string}")
    public void vendedorConsultaCentro(String centro) {
        api.getConToken(tokenVendedor(), "/nodos/" + nodoCentro(centro) + "/inventario");
    }

    @Cuando("el operador de centro de distribución consulta el inventario del nodo del centro de distribución {string}")
    public void operadorConsultaCentro(String centro) {
        api.getConToken(tokenOperador(), "/nodos/" + nodoCentro(centro) + "/inventario");
    }

    @Cuando("el usuario sin permiso de consulta consulta el inventario del almacén de la tienda {string}")
    public void usuarioSinConsulta(String tienda) {
        api.getConToken(contexto.texto("usuarioSinConsulta:token"),
                "/nodos/" + nodoAlmacen(tienda) + "/inventario");
    }

    // ------------------------------------------------------------------ aserciones de inventario

    @Entonces("el campo {string} es el identificador de la tienda {string}")
    public void campoEsIdTienda(String ruta, String tienda) {
        campoEs(ruta, idTienda(tienda));
    }

    @Entonces("el campo {string} es el identificador de la bodega de la tienda {string}")
    public void campoEsIdBodega(String ruta, String tienda) {
        campoEs(ruta, idBodega(tienda));
    }

    @Entonces("el campo {string} es el identificador del almacén de la tienda {string}")
    public void campoEsIdAlmacen(String ruta, String tienda) {
        campoEs(ruta, nodoAlmacen(tienda));
    }

    @Entonces("el campo {string} es el identificador del centro de distribución {string}")
    public void campoEsIdCentro(String ruta, String centro) {
        campoEs(ruta, idCentro(centro));
    }

    @Entonces("el campo {string} es el identificador del nodo del centro de distribución {string}")
    public void campoEsNodoCentro(String ruta, String centro) {
        campoEs(ruta, nodoCentro(centro));
    }

    @Entonces("el campo {string} es el identificador del último centro de distribución registrado")
    public void campoEsUltimoCentro(String ruta) {
        campoEs(ruta, contexto.id("ultimoCdId"));
    }

    @Entonces("el campo {string} es el identificador de la tienda recién registrada")
    public void campoEsUltimaTienda(String ruta) {
        campoEs(ruta, contexto.id("ultimaTiendaId"));
    }

    @Entonces("el campo {string} es el nombre de la tienda {string}")
    public void campoEsNombreTienda(String ruta, String tienda) {
        campoEs(ruta, contexto.texto("tienda:" + tienda + ":nombre"));
    }

    @Entonces("el campo {string} es el nombre del centro de distribución {string}")
    public void campoEsNombreCentro(String ruta, String centro) {
        campoEs(ruta, contexto.texto("cd:" + centro + ":nombre"));
    }

    @Entonces("el campo {string} es el nombre del último centro de distribución registrado")
    public void campoEsNombreUltimoCentro(String ruta) {
        campoEs(ruta, contexto.texto("ultimoCdNombre"));
    }

    @Entonces("el campo {string} es el nombre nuevo de la tienda {string}")
    public void campoEsNombreNuevoTienda(String ruta, String tienda) {
        campoEs(ruta, contexto.texto("tienda:" + tienda + ":nombreNuevo"));
    }

    @Entonces("el campo {string} es el nombre nuevo del centro de distribución {string}")
    public void campoEsNombreNuevoCentro(String ruta, String centro) {
        campoEs(ruta, contexto.texto("cd:" + centro + ":nombreNuevo"));
    }

    @Entonces("el almacén y la bodega de la tienda {string} son nodos distintos")
    public void almacenYBodegaSonDistintos(String tienda) {
        assertThat(nodoAlmacen(tienda)).isNotNull();
        assertThat(nodoBodega(tienda)).isNotNull();
        assertThat(nodoAlmacen(tienda)).isNotEqualTo(nodoBodega(tienda));
    }

    @Entonces("el listado de tiendas no expone el almacén de la primera tienda")
    public void listadoSinAlmacen() {
        contexto.getRespuesta().then()
                .body("content[0].id", org.hamcrest.Matchers.notNullValue())
                .body("content[0].almacenId", nullValue());
    }

    @Entonces("el inventario del nodo contiene la variante {string} con {int} unidades a la mano")
    public void inventarioDelNodoContiene(String variante, int cantidad) {
        contexto.getRespuesta().then().body(
                "content.find { it.varianteId == '" + idVariante(variante) + "' }.aLaMano",
                equalTo(cantidad));
    }

    @Entonces("el inventario de la variante {string} tiene {int} filas")
    public void inventarioDeLaVarianteTieneFilas(String variante, int filas) {
        contexto.getRespuesta().then().body("$", hasSize(filas));
    }

    @Entonces("el inventario de la variante {string} solo muestra el almacén de la tienda {string}")
    public void inventarioDeLaVarianteSoloMuestraTienda(String variante, String tienda) {
        contexto.getRespuesta().then()
                .body("$", hasSize(1))
                .body("[0].nodoId", equalTo(nodoAlmacen(tienda)));
    }

    @Entonces("el stock de la variante {string} en el almacén de la tienda {string} es {int}")
    public void stockEnAlmacen(String variante, String tienda, int cantidad) {
        assertThat(soporte.stock(tokenAdmin(), nodoAlmacen(tienda), idVariante(variante))).isEqualTo(cantidad);
    }

    @Entonces("el stock de la variante {string} en la bodega de la tienda {string} es {int}")
    public void stockEnBodega(String variante, String tienda, int cantidad) {
        assertThat(soporte.stock(tokenAdmin(), nodoBodega(tienda), idVariante(variante))).isEqualTo(cantidad);
    }

    @Entonces("el stock de la variante {string} en el nodo del centro de distribución {string} es {int}")
    public void stockEnCentro(String variante, String centro, int cantidad) {
        assertThat(soporte.stock(tokenAdmin(), nodoCentro(centro), idVariante(variante))).isEqualTo(cantidad);
    }

    @Entonces("el stock de la variante {string} en el almacén de la tienda {string} es {int} menos las ventas confirmadas")
    public void stockMenosVentasConfirmadas(String variante, String tienda, int base) {
        assertThat(soporte.stock(tokenAdmin(), nodoAlmacen(tienda), idVariante(variante)))
                .isEqualTo(base - ventasConfirmadas);
    }

    @Entonces("la variante {string} tiene fila de inventario en el almacén de la tienda {string}")
    public void varianteTieneFila(String variante, String tienda) {
        assertThat(soporte.tieneFila(tokenAdmin(), nodoAlmacen(tienda), idVariante(variante))).isTrue();
    }

    @Entonces("la variante {string} no tiene fila de inventario en el almacén de la tienda {string}")
    public void varianteNoTieneFila(String variante, String tienda) {
        assertThat(soporte.tieneFila(tokenAdmin(), nodoAlmacen(tienda), idVariante(variante))).isFalse();
    }

    // ------------------------------------------------------------------ aserciones de kardex y errores

    @Entonces("todos los movimientos del kardex tienen fecha en UTC")
    public void movimientosEnUtc() {
        contexto.getRespuesta().then().body("content.ocurridoEn", everyItem(endsWith("Z")));
    }

    @Entonces("el kardex registra los movimientos {string}")
    public void kardexRegistraMovimientos(String movimientos) {
        List<String> tipos = contexto.getRespuesta().then().statusCode(200)
                .extract().path("content.tipo");
        org.hamcrest.MatcherAssert.assertThat(tipos,
                org.hamcrest.Matchers.contains(movimientos.split("\\s*,\\s*")));
    }

    @Entonces("el kardex tiene {int} movimientos")
    public void kardexTieneMovimientos(int movimientos) {
        contexto.getRespuesta().then().body("content", hasSize(movimientos));
    }

    @Entonces("el kardex contiene un único movimiento de tipo {string} con motivo {string}")
    public void kardexUnicoMovimiento(String tipo, String motivo) {
        contexto.getRespuesta().then()
                .body("content", hasSize(1))
                .body("content[0].tipo", equalTo(tipo))
                .body("content[0].motivo", equalTo(motivo));
    }

    @Entonces("el kardex está vacío")
    public void kardexVacio() {
        contexto.getRespuesta().then().body("content", hasSize(0));
    }

    @Entonces("la respuesta es {int} o {int}")
    public void laRespuestaEsUnoDe(int primero, int segundo) {
        int codigo = contexto.getRespuesta().statusCode();
        assertThat(codigo).isIn(primero, segundo);
    }

    @Entonces("el mensaje de error menciona {string}")
    public void elMensajeMenciona(String texto) {
        contexto.getRespuesta().then().body("mensaje", containsString(texto));
    }

    @Entonces("todas las ventas simultáneas finalizan con código 201 o 409")
    public void ventasSimultaneasConCodigoValido() {
        assertThat(ventasConCodigoValido).isTrue();
    }

    @Entonces("todos los reintentos en paralelo finalizan con código 200, 201 o 409")
    public void reintentosConCodigoValido() {
        assertThat(reintentosConCodigoValido).isTrue();
    }

    @Entonces("el reintento final confirma la venta")
    public void reintentoFinalConfirma() {
        assertThat(codigoFinalReintento).isIn(200, 201);
    }

    // ------------------------------------------------------------------ utilidades

    private String tokenAdmin() {
        return contexto.getToken();
    }

    private String tokenVendedor() {
        return contexto.texto("vendedor:token");
    }

    private String tokenOperador() {
        return contexto.texto("operadorCd:token");
    }

    private String tokenSupervisor() {
        return contexto.texto("supervisor:token");
    }

    private String idTienda(String alias) {
        return contexto.id("tienda:" + alias);
    }

    private String idBodega(String alias) {
        return contexto.id("tienda:" + alias + ":bodega");
    }

    private String nodoAlmacen(String alias) {
        return contexto.id("tienda:" + alias + ":almacen");
    }

    private String nodoBodega(String alias) {
        return contexto.id("tienda:" + alias + ":bodega");
    }

    private String idCentro(String alias) {
        return contexto.id("cd:" + alias);
    }

    private String nodoCentro(String alias) {
        return contexto.id("cd:" + alias + ":nodo");
    }

    private String idVariante(String alias) {
        return contexto.id("variante:" + alias);
    }

    private String skuDe(String referencia) {
        String sku = contexto.texto("variante:" + referencia + ":sku");
        return sku == null ? referencia : sku;
    }

    private List<Map<String, Object>> itemsDe(DataTable tabla) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, String> fila : tabla.asMaps(String.class, String.class)) {
            String referencia = fila.get("variante");
            int cantidad = Integer.parseInt(fila.get("cantidad").trim());
            items.add(InventarioSoporte.item(skuDe(referencia), cantidad));
        }
        return items;
    }

    private void campoEs(String ruta, String valor) {
        contexto.getRespuesta().then().body(ruta, equalTo(valor));
    }
}
