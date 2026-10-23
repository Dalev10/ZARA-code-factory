package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import com.codefactory.supplychain.qa.soporte.ReposicionSoporte;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Pasos de los escenarios de reposición: umbrales, traspasos automáticos y manuales,
 * evaluación masiva y prioridad de centros de distribución por tienda.
 */
public class PasosReposicion {

    @Autowired
    private ContextoPrueba contexto;

    @Autowired
    private ReposicionSoporte soporte;

    // ------------------------------------------------------------------ umbrales

    @Dado("que existe un umbral para la variante {string} con mínimo {int} y objetivo {int}")
    public void existeUmbralParaVariante(String variante, int minimo, int objetivo) {
        soporte.crearUmbralVariante(variante, minimo, objetivo);
    }

    @Dado("que existe un umbral para la categoría {string} con mínimo {int} y objetivo {int}")
    public void existeUmbralParaCategoria(String categoria, int minimo, int objetivo) {
        soporte.crearUmbralCategoria(categoria, minimo, objetivo);
    }

    @Cuando("registro un umbral para la variante {string} con mínimo {int} y objetivo {int}")
    public void registrarUmbralParaVariante(String variante, int minimo, int objetivo) {
        soporte.postUmbralVariante(variante, minimo, objetivo);
    }

    @Cuando("registro un umbral para la categoría {string} con mínimo {int} y objetivo {int}")
    public void registrarUmbralParaCategoria(String categoria, int minimo, int objetivo) {
        soporte.postUmbralCategoria(categoria, minimo, objetivo);
    }

    @Cuando("registro un umbral para la variante {string} y la categoría {string} con mínimo {int} y objetivo {int}")
    public void registrarUmbralConDosAlcances(String variante, String categoria, int minimo, int objetivo) {
        soporte.postUmbralMixto(variante, categoria, minimo, objetivo);
    }

    @Cuando("registro un umbral sin variante ni categoría con mínimo {int} y objetivo {int}")
    public void registrarUmbralSinAlcance(int minimo, int objetivo) {
        soporte.postUmbralSinAlcance(minimo, objetivo);
    }

    @Cuando("registro un umbral para una variante inexistente con mínimo {int} y objetivo {int}")
    public void registrarUmbralParaVarianteInexistente(int minimo, int objetivo) {
        soporte.postUmbralVarianteInexistente(minimo, objetivo);
    }

    @Cuando("el supervisor {string} intenta registrar un umbral para la variante {string} con mínimo {int} y objetivo {int}")
    public void supervisorRegistraUmbral(String supervisor, String variante, int minimo, int objetivo) {
        soporte.postUmbralComo(soporte.tokenSupervisor(supervisor), variante, minimo, objetivo);
    }

    @Cuando("modifico el umbral de la variante {string} a mínimo {int} y objetivo {int}")
    public void modificarUmbralDeVariante(String variante, int minimo, int objetivo) {
        soporte.actualizarUmbralVariante(variante, minimo, objetivo);
    }

    @Cuando("elimino el umbral de la variante {string}")
    public void eliminarUmbralDeVariante(String variante) {
        soporte.eliminarUmbralVariante(variante);
    }

    @Cuando("consulto el umbral de la variante {string}")
    public void consultarUmbralDeVariante(String variante) {
        soporte.consultarUmbralVariante(variante);
    }

    @Cuando("consulto el umbral creado")
    public void consultarUmbralCreado() {
        soporte.consultarUmbralCreado();
    }

    @Entonces("el umbral pertenece a la variante {string}")
    public void umbralPerteneceALaVariante(String variante) {
        contexto.getRespuesta().then()
                .body("varianteId", equalTo(soporte.idVariante(variante)))
                .body("categoriaId", nullValue());
    }

    @Entonces("el umbral pertenece a la categoría {string}")
    public void umbralPerteneceALaCategoria(String categoria) {
        contexto.getRespuesta().then()
                .body("categoriaId", equalTo(soporte.idCategoria(categoria)))
                .body("varianteId", nullValue());
    }

    // ------------------------------------------------------------------ traspaso automático

    @Dado("que la tienda {string} tiene {int} unidad(es) de la variante {string} en el almacén")
    public void tiendaTieneUnidadesEnAlmacen(String tienda, int cantidad, String variante) {
        soporte.cargarEnAlmacen(tienda, variante, cantidad);
    }

    @Dado("que la tienda {string} tiene {int} unidad(es) de la variante {string} en la bodega")
    public void tiendaTieneUnidadesEnBodega(String tienda, int cantidad, String variante) {
        soporte.cargarEnBodega(tienda, variante, cantidad);
    }

    @Cuando("el vendedor asignado registra una venta de {int} unidad(es) de la variante {string} en la tienda {string}")
    public void vendedorAsignadoRegistraVenta(int cantidad, String variante, String tienda) {
        soporte.vender(contexto.texto("vendedor:token"), tienda, variante, cantidad, false);
    }

    @Cuando("se registra una venta de {int} unidad(es) de la variante {string} en la tienda {string}")
    public void registrarVenta(int cantidad, String variante, String tienda) {
        soporte.vender(contexto.getToken(), tienda, variante, cantidad, false);
    }

    @Cuando("se registra una venta forzada de {int} unidad(es) de la variante {string} en la tienda {string}")
    public void registrarVentaForzada(int cantidad, String variante, String tienda) {
        soporte.vender(contexto.getToken(), tienda, variante, cantidad, true);
    }

    @Cuando("se cargan {int} unidades de la variante {string} en la bodega de la tienda {string}")
    public void cargarUnidadesEnBodega(int cantidad, String variante, String tienda) {
        soporte.cargarEnBodega(tienda, variante, cantidad);
    }

    @Cuando("se registra un ajuste positivo de {int} unidades de la variante {string} en la bodega de la tienda {string} con motivo {string}")
    public void ajustarPositivamenteLaBodega(int cantidad, String variante, String tienda, String motivo) {
        soporte.ajustarBodega(tienda, variante, cantidad, motivo);
    }

    @Entonces("el almacén de la tienda {string} tiene {int} unidad(es) de la variante {string}")
    public void almacenTieneUnidades(String tienda, int cantidad, String variante) {
        assertThat(soporte.stock(tienda, variante, true)).isEqualTo(cantidad);
    }

    @Entonces("la bodega de la tienda {string} tiene {int} unidad(es) de la variante {string}")
    public void bodegaTieneUnidades(String tienda, int cantidad, String variante) {
        assertThat(soporte.stock(tienda, variante, false)).isEqualTo(cantidad);
    }

    @Entonces("la tienda {string} no tiene traspasos registrados")
    public void tiendaSinTraspasos(String tienda) {
        assertThat(soporte.traspasos(tienda)).isEmpty();
    }

    @Entonces("la tienda {string} tiene {int} traspaso(s) registrado(s)")
    public void tiendaConTraspasos(String tienda, int cantidad) {
        assertThat(soporte.traspasos(tienda)).hasSize(cantidad);
    }

    @Entonces("la tienda {string} tiene {int} traspaso(s) automático(s)")
    public void tiendaConTraspasosAutomaticos(String tienda, int cantidad) {
        long automaticos = soporte.traspasos(tienda).stream()
                .filter(traspaso -> "AUTOMATICO".equals(traspaso.get("origen")))
                .count();
        assertThat(automaticos).isEqualTo(cantidad);
    }

    @Entonces("el traspaso número {int} de la tienda {string} es automático con cantidad {int} de la variante {string}")
    public void traspasoAutomaticoConCantidad(int numero, String tienda, int cantidad, String variante) {
        List<Map<String, Object>> traspasos = soporte.traspasos(tienda);
        assertThat(traspasos).hasSizeGreaterThanOrEqualTo(numero);
        Map<String, Object> traspaso = traspasos.get(numero - 1);
        assertThat(traspaso.get("origen")).isEqualTo("AUTOMATICO");
        assertThat(((Number) traspaso.get("cantidad")).intValue()).isEqualTo(cantidad);
        assertThat(traspaso.get("varianteId")).isEqualTo(soporte.idVariante(variante));
    }

    @Entonces("el traspaso número {int} de la tienda {string} tiene cantidad {int}")
    public void traspasoConCantidad(int numero, String tienda, int cantidad) {
        List<Map<String, Object>> traspasos = soporte.traspasos(tienda);
        assertThat(traspasos).hasSizeGreaterThanOrEqualTo(numero);
        assertThat(((Number) traspasos.get(numero - 1).get("cantidad")).intValue())
                .isEqualTo(cantidad);
    }

    @Entonces("la ventana de quiebre de la variante {string} en la tienda {string} está abierta")
    public void ventanaDeQuiebreAbierta(String variante, String tienda) {
        Map<String, Object> ventana = soporte.ventana(tienda, variante);
        assertThat(ventana).isNotNull();
        assertThat(ventana.get("estado")).isEqualTo("ABIERTA");
    }

    @Entonces("la ventana de quiebre de la variante {string} en la tienda {string} está cerrada con motivo {string}")
    public void ventanaDeQuiebreCerrada(String variante, String tienda, String motivo) {
        Map<String, Object> ventana = soporte.ventana(tienda, variante);
        assertThat(ventana).isNotNull();
        assertThat(ventana.get("estado")).isEqualTo("CERRADA");
        assertThat(ventana.get("motivoCierre")).isEqualTo(motivo);
    }

    // ------------------------------------------------------------------ traspaso manual

    @Cuando("registro un traspaso manual de {int} unidad(es) de la variante {string} en la tienda {string}")
    public void registrarTraspasoManual(int cantidad, String variante, String tienda) {
        soporte.traspasoManual(contexto.getToken(), tienda, variante, cantidad);
    }

    @Cuando("registro un traspaso manual sin variante con cantidad {int} en la tienda {string}")
    public void registrarTraspasoManualSinVariante(int cantidad, String tienda) {
        soporte.traspasoManualSinVariante(contexto.getToken(), tienda, cantidad);
    }

    @Cuando("registro un traspaso manual de {int} unidad(es) de una variante inexistente en la tienda {string}")
    public void registrarTraspasoManualDeVarianteInexistente(int cantidad, String tienda) {
        soporte.traspasoManualVarianteInexistente(contexto.getToken(), tienda, cantidad);
    }

    @Cuando("el usuario {string} registra un traspaso manual de {int} unidad(es) de la variante {string} en la tienda {string}")
    public void usuarioRegistraTraspasoManual(String usuario, int cantidad, String variante, String tienda) {
        soporte.traspasoManual(soporte.tokenUsuario(usuario), tienda, variante, cantidad);
    }

    @Cuando("el usuario {string} lista los traspasos de la tienda {string}")
    public void usuarioListaTraspasos(String usuario, String tienda) {
        soporte.consultarTraspasosComo(soporte.tokenUsuario(usuario), tienda);
    }

    @Cuando("listo los traspasos de la tienda {string}")
    public void listarTraspasos(String tienda) {
        soporte.traspasos(tienda);
    }

    @Cuando("consulto el traspaso número {int} de la tienda {string}")
    public void consultarTraspaso(int numero, String tienda) {
        soporte.consultarTraspaso(numero);
    }

    @Cuando("consulto un traspaso inexistente")
    public void consultarTraspasoInexistente() {
        soporte.consultarTraspasoInexistente();
    }

    @Cuando("consulto el kardex de la bodega de la tienda {string} con tipo {string}")
    public void consultarKardexDeLaBodega(String tienda, String tipo) {
        soporte.kardex(tienda, false, tipo);
    }

    @Cuando("consulto el kardex del almacén de la tienda {string} con tipo {string}")
    public void consultarKardexDelAlmacen(String tienda, String tipo) {
        soporte.kardex(tienda, true, tipo);
    }

    @Entonces("el kardex tiene {int} movimiento")
    public void kardexTieneMovimientos(int cantidad) {
        contexto.getRespuesta().then().body("content", org.hamcrest.Matchers.hasSize(cantidad));
    }

    @Entonces("el traspaso pertenece a la tienda {string}")
    public void traspasoPerteneceALaTienda(String tienda) {
        contexto.getRespuesta().then().body("tiendaId", equalTo(soporte.idTienda(tienda)));
    }

    @Entonces("el traspaso fue registrado por el administrador")
    public void traspasoRegistradoPorElAdministrador() {
        Response respuesta = contexto.getRespuesta();
        String administradorId = soporte.idAdministrador();
        respuesta.then().body("registradoPor", equalTo(administradorId));
        contexto.setRespuesta(respuesta);
    }

    @Entonces("el traspaso consultado es el número {int} de la tienda {string}")
    public void traspasoConsultadoEsElNumero(int numero, String tienda) {
        assertThat(soporte.tiendaDelTraspaso(numero)).isEqualTo(tienda);
        contexto.getRespuesta().then().body("id", equalTo(soporte.idTraspaso(numero)));
    }

    // ------------------------------------------------------------------ evaluación masiva

    @Cuando("evalúo la reposición de la tienda {string}")
    public void evaluarReposicionDeLaTienda(String tienda) {
        soporte.evaluarTienda(tienda);
    }

    @Cuando("evalúo la reposición de todas las tiendas activas")
    public void evaluarReposicionDeTodasLasTiendas() {
        soporte.evaluarTodas();
    }

    @Cuando("evalúo la reposición de una tienda inexistente")
    public void evaluarReposicionDeTiendaInexistente() {
        soporte.evaluarTiendaInexistente();
    }

    @Cuando("el supervisor {string} evalúa la reposición de todas las tiendas activas")
    public void supervisorEvaluaReposicion(String supervisor) {
        soporte.evaluarTodasComo(soporte.tokenSupervisor(supervisor));
    }

    @Cuando("desactivo la tienda {string}")
    public void desactivarTienda(String tienda) {
        soporte.desactivarTienda(tienda);
    }

    @Entonces("se ejecutaron al menos {int} traspasos")
    public void seEjecutaronAlMenosTraspasos(int cantidad) {
        contexto.getRespuesta().then()
                .body("traspasoEjecutados", greaterThanOrEqualTo(cantidad));
    }

    // ------------------------------------------------------------------ prioridad de CDs

    @Cuando("defino la prioridad de CDs de la tienda {string} como {string}, {string}, {string}")
    public void definirPrioridadConTresCds(String tienda, String cd1, String cd2, String cd3) {
        soporte.definirPrioridad(tienda, List.of(cd1, cd2, cd3));
    }

    @Cuando("defino la prioridad de CDs de la tienda {string} como {string}, {string}")
    public void definirPrioridadConDosCds(String tienda, String cd1, String cd2) {
        soporte.definirPrioridad(tienda, List.of(cd1, cd2));
    }

    @Cuando("defino la prioridad de CDs de la tienda {string} como {string}")
    public void definirPrioridadConUnCd(String tienda, String cd) {
        soporte.definirPrioridad(tienda, List.of(cd));
    }

    @Cuando("defino la prioridad de CDs de la tienda {string} con una lista vacía")
    public void definirPrioridadVacia(String tienda) {
        soporte.definirPrioridad(tienda, List.of());
    }

    @Cuando("defino la prioridad de CDs de la tienda {string} con un CD inexistente")
    public void definirPrioridadConCdInexistente(String tienda) {
        soporte.definirPrioridadConIds(soporte.idTienda(tienda),
                List.of(UUID.randomUUID().toString()));
    }

    @Cuando("defino la prioridad de CDs de una tienda inexistente como {string}")
    public void definirPrioridadDeTiendaInexistente(String cd) {
        soporte.definirPrioridadConIds(UUID.randomUUID().toString(),
                List.of(soporte.idCentro(cd)));
    }

    @Cuando("defino la prioridad de CDs de la tienda {string} sin la lista de CDs")
    public void definirPrioridadSinLista(String tienda) {
        soporte.definirPrioridadSinLista(tienda);
    }

    @Cuando("el vendedor asignado define la prioridad de CDs de la tienda {string} como {string}")
    public void vendedorDefinePrioridad(String tienda, String cd) {
        soporte.definirPrioridadComo(contexto.texto("vendedor:token"), tienda, List.of(cd));
    }

    @Cuando("consulto la prioridad de CDs de la tienda {string}")
    public void consultarPrioridadDeCds(String tienda) {
        soporte.consultarPrioridad(tienda);
    }

    @Entonces("la prioridad de CDs de la tienda {string} es {string}, {string}, {string}")
    public void prioridadDeCdsEnOrden(String tienda, String cd1, String cd2, String cd3) {
        assertThat(soporte.idsPrioridad()).containsExactly(
                soporte.idCentro(cd1), soporte.idCentro(cd2), soporte.idCentro(cd3));
    }

    @Entonces("la tienda {string} tiene {int} CD(s) prioritario(s): {string}")
    public void tiendaTieneCdPrioritario(String tienda, int cantidad, String cd) {
        List<String> prioridades = soporte.idsPrioridad();
        assertThat(prioridades).hasSize(cantidad);
        assertThat(prioridades).containsExactly(soporte.idCentro(cd));
    }

    @Entonces("la tienda {string} no tiene CDs prioritarios")
    public void tiendaSinCdsPrioritarios(String tienda) {
        assertThat(soporte.idsPrioridad()).isEmpty();
    }

    @Entonces("el CD repetido es rechazado con 400 o 409")
    public void cdRepetidoRechazado() {
        assertThat(contexto.getRespuesta().statusCode()).isIn(400, 409);
    }

    // ------------------------------------------------------------------ usuarios de reposición

    @Dado("que existe un supervisor de reposición llamado {string}")
    public void existeSupervisorDeReposicion(String supervisor) {
        soporte.crearSupervisor(supervisor);
    }

    @Dado("que existe un usuario de reposición con el scope {string} llamado {string}")
    public void existeUsuarioDeReposicionConScope(String scope, String usuario) {
        soporte.crearUsuarioConScope(usuario, scope);
    }
}
