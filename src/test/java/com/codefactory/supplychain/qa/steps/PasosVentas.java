package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import com.codefactory.supplychain.qa.soporte.VentasDatos;
import com.codefactory.supplychain.qa.soporte.VentasDatos.LineaVenta;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;

public class PasosVentas {

    private static final String MOTIVO_INGRESO_MANUAL = "INGRESO_MANUAL";

    @Autowired
    private VentasDatos ventas;

    @Autowired
    private ContextoPrueba contexto;

    @Dado("que existe una tienda de ventas llamada {string}")
    public void crearTienda(String alias) {
        ventas.crearTienda(alias);
    }

    @Dado("que existe una variante de ventas llamada {string}")
    public void crearVariante(String alias) {
        ventas.crearVariante(alias);
    }

    @Dado("que la variante {string} tiene {int} unidades en el almacén de la tienda {string}")
    public void cargarInventario(String variante, int cantidad, String tienda) {
        ventas.cargar(tienda, variante, cantidad);
    }

    @Dado("que existe un vendedor llamado {string} asignado a la tienda {string}")
    public void crearVendedorAsignado(String alias, String tienda) {
        ventas.usuarioConRol(alias, "VENDEDOR");
        ventas.asignarTienda(alias, tienda);
    }

    @Dado("que existe un vendedor llamado {string} sin tiendas asignadas")
    public void crearVendedorSinTienda(String alias) {
        ventas.usuarioConRol(alias, "VENDEDOR");
    }

    @Dado("que existe un supervisor llamado {string} asignado a la tienda {string}")
    public void crearSupervisorAsignado(String alias, String tienda) {
        ventas.usuarioConRol(alias, "SUPERVISOR_TIENDA");
        ventas.asignarTienda(alias, tienda);
    }

    @Dado("que existe un supervisor llamado {string} sin tiendas asignadas")
    public void crearSupervisorSinTienda(String alias) {
        ventas.usuarioConRol(alias, "SUPERVISOR_TIENDA");
    }

    @Dado("que existe un usuario de ventas llamado {string} con el permiso {string}")
    public void crearUsuarioConPermiso(String alias, String permiso) {
        ventas.usuarioConScopes(alias, permiso);
    }

    @Dado("que la tienda {string} está asignada al usuario {string}")
    public void asignarTienda(String tienda, String usuario) {
        ventas.asignarTienda(usuario, tienda);
    }

    @Cuando("registro una venta en la tienda {string} con la transacción {string} y la variante {string} y cantidad {int}")
    public void registrarVenta(String tienda, String transaccion, String variante, int cantidad) {
        ventas.vender(contexto.getToken(), tienda, transaccion, false, List.of(new LineaVenta(variante, cantidad)));
    }

    @Cuando("registro una venta como {string} en la tienda {string} con la transacción {string} y la variante {string} y cantidad {int}")
    public void registrarVentaComo(String usuario, String tienda, String transaccion, String variante, int cantidad) {
        ventas.vender(ventas.tokenDe(usuario), tienda, transaccion, false,
                List.of(new LineaVenta(variante, cantidad)));
    }

    @Cuando("registro una venta forzada en la tienda {string} con la transacción {string} y la variante {string} y cantidad {int}")
    public void registrarVentaForzada(String tienda, String transaccion, String variante, int cantidad) {
        ventas.vender(contexto.getToken(), tienda, transaccion, true, List.of(new LineaVenta(variante, cantidad)));
    }

    @Cuando("registro una venta forzada como {string} en la tienda {string} con la transacción {string} y la variante {string} y cantidad {int}")
    public void registrarVentaForzadaComo(String usuario, String tienda, String transaccion, String variante,
                                          int cantidad) {
        ventas.vender(ventas.tokenDe(usuario), tienda, transaccion, true,
                List.of(new LineaVenta(variante, cantidad)));
    }

    @Cuando("registro una venta en la tienda {string} con la transacción {string} y las líneas:")
    public void registrarVentaConLineas(String tienda, String transaccion, DataTable tabla) {
        List<LineaVenta> lineas = new ArrayList<>();
        for (Map<String, String> fila : tabla.asMaps(String.class, String.class)) {
            lineas.add(new LineaVenta(fila.get("variante"), Integer.parseInt(fila.get("cantidad"))));
        }
        ventas.vender(contexto.getToken(), tienda, transaccion, false, lineas);
    }

    @Cuando("registro otra venta en la tienda {string} con la transacción {string} y la variante {string} y cantidad {int}")
    public void registrarOtraVenta(String tienda, String transaccion, String variante, int cantidad) {
        ventas.vender(contexto.getToken(), tienda, transaccion, false, List.of(new LineaVenta(variante, cantidad)));
    }

    @Cuando("registro una venta en la tienda {string} con la transacción {string} y el SKU {string} y cantidad {int}")
    public void registrarVentaConSku(String tienda, String transaccion, String sku, int cantidad) {
        ventas.venderConSku(contexto.getToken(), tienda, transaccion, sku, cantidad);
    }

    @Cuando("registro una venta en la tienda {string} con la transacción {string} y la variante {string} con precio unitario {double}")
    public void registrarVentaConPrecio(String tienda, String transaccion, String variante, double precio) {
        ventas.venderConPrecio(contexto.getToken(), tienda, transaccion, variante, 1,
                String.valueOf(precio));
    }

    @Cuando("registro una venta en la tienda {string} con la transacción {string} sin líneas")
    public void registrarVentaSinLineas(String tienda, String transaccion) {
        ventas.venderSinLineas(contexto.getToken(), tienda, transaccion);
    }

    @Cuando("registro una venta sin canal ni forzarSinStock en la tienda {string} con la transacción {string} y la variante {string} y cantidad {int}")
    public void registrarVentaSinOpcionales(String tienda, String transaccion, String variante, int cantidad) {
        ventas.venderSinOpcionales(contexto.getToken(), tienda, transaccion, variante, cantidad);
    }

    @Cuando("repito la última venta registrada")
    public void repetirUltimaVenta() {
        ventas.repetirUltimaVenta();
    }

    @Cuando("intento cargar {int} unidades de la variante {string} en el almacén de la tienda {string}")
    public void intentarCargar(int cantidad, String variante, String tienda) {
        ventas.cargaInicial(tienda, variante, cantidad);
    }

    @Cuando("cargo {int} unidades de la variante {string} en el almacén de la tienda {string}")
    public void cargar(int cantidad, String variante, String tienda) {
        ventas.cargar(tienda, variante, cantidad);
    }

    @Cuando("ajusto el inventario de la variante {string} en la tienda {string} en {int} unidades con el motivo {string}")
    public void ajustarInventario(String variante, String tienda, int delta, String motivo) {
        ventas.ajustar(tienda, variante, delta, motivo);
    }

    @Cuando("repongo el almacén de la tienda {string} con {int} unidades de la variante {string}")
    public void reponerAlmacen(String tienda, int cantidad, String variante) {
        ventas.ajustar(tienda, variante, cantidad, MOTIVO_INGRESO_MANUAL);
    }

    @Cuando("anulo la línea de venta {string} con el motivo {string}")
    public void anularLinea(String linea, String motivo) {
        ventas.anularLinea(contexto.getToken(), linea, motivo(motivo));
    }

    @Cuando("anulo la línea de venta {string} como {string} con el motivo {string}")
    public void anularLineaComo(String linea, String usuario, String motivo) {
        ventas.anularLinea(ventas.tokenDe(usuario), linea, motivo(motivo));
    }

    @Cuando("anulo la línea de venta {string} sin motivo")
    public void anularLineaSinMotivo(String linea) {
        ventas.anularLinea(contexto.getToken(), linea, "{}");
    }

    @Cuando("anulo la línea de venta {string} con motivo en blanco")
    public void anularLineaConMotivoEnBlanco(String linea) {
        ventas.anularLinea(contexto.getToken(), linea, motivo("  "));
    }

    @Cuando("anulo la transacción {string} con el motivo {string}")
    public void anularTransaccion(String transaccion, String motivo) {
        ventas.anularTransaccion(contexto.getToken(), transaccion, motivo(motivo));
    }

    @Cuando("anulo una línea de venta inexistente con el motivo {string}")
    public void anularLineaInexistente(String motivo) {
        ventas.anularLineaInexistente(contexto.getToken(), motivo(motivo));
    }

    @Cuando("anulo una transacción de venta inexistente con el motivo {string}")
    public void anularTransaccionInexistente(String motivo) {
        ventas.anularTransaccionInexistente(contexto.getToken(), motivo(motivo));
    }

    @Cuando("consulto las ventas de la tienda {string}")
    public void consultarVentasDeTienda(String tienda) {
        ventas.consultarVentas(contexto.getToken(), Map.of("tiendaId", contexto.id("tienda:" + tienda)));
    }

    @Cuando("consulto las ventas de la tienda {string} como {string}")
    public void consultarVentasDeTiendaComo(String tienda, String usuario) {
        ventas.consultarVentas(ventas.tokenDe(usuario), Map.of("tiendaId", contexto.id("tienda:" + tienda)));
    }

    @Cuando("consulto las ventas de la tienda {string} de la variante {string}")
    public void consultarVentasDeTiendaYVariante(String tienda, String variante) {
        ventas.consultarVentas(contexto.getToken(), Map.of(
                "tiendaId", contexto.id("tienda:" + tienda),
                "varianteId", contexto.id("variante:" + variante)));
    }

    @Cuando("consulto las ventas de la tienda {string} con estado {string}")
    public void consultarVentasDeTiendaConEstado(String tienda, String estado) {
        ventas.consultarVentas(contexto.getToken(), Map.of(
                "tiendaId", contexto.id("tienda:" + tienda),
                "estado", estado));
    }

    @Cuando("consulto las ventas de la tienda {string} desde una fecha futura")
    public void consultarVentasDesdeFechaFutura(String tienda) {
        ventas.consultarVentasDesdeFuturo(contexto.getToken(), tienda);
    }

    @Cuando("consulto las ventas con la transacción {string}")
    public void consultarVentasPorTransaccion(String transaccion) {
        ventas.consultarVentas(contexto.getToken(), Map.of("transaccionId", ventas.transaccion(transaccion)));
    }

    @Cuando("consulto las ventas de la variante {string} como {string}")
    public void consultarVentasDeVarianteComo(String variante, String usuario) {
        ventas.consultarVentas(ventas.tokenDe(usuario), Map.of("varianteId", contexto.id("variante:" + variante)));
    }

    @Cuando("consulto la línea de venta {string}")
    public void consultarLinea(String linea) {
        ventas.consultarLinea(contexto.getToken(), linea);
    }

    @Cuando("consulto la línea de venta {string} como {string}")
    public void consultarLineaComo(String linea, String usuario) {
        ventas.consultarLinea(ventas.tokenDe(usuario), linea);
    }

    @Cuando("registro una solicitud no satisfecha en la tienda {string} para la variante {string} con cantidad {int} y clave {string}")
    public void registrarSolicitud(String tienda, String variante, int cantidad, String claveAlias) {
        ventas.registrarSolicitud(contexto.getToken(), tienda, variante, cantidad, claveAlias, ventas.clave(claveAlias));
    }

    @Cuando("registro una solicitud no satisfecha como {string} en la tienda {string} para la variante {string} con cantidad {int} y clave {string}")
    public void registrarSolicitudComo(String usuario, String tienda, String variante, int cantidad, String claveAlias) {
        ventas.registrarSolicitud(ventas.tokenDe(usuario), tienda, variante, cantidad, claveAlias,
                ventas.clave(claveAlias));
    }

    @Cuando("registro una solicitud no satisfecha en la tienda {string} para la variante {string} sin cantidad y clave {string}")
    public void registrarSolicitudSinCantidad(String tienda, String variante, String claveAlias) {
        ventas.registrarSolicitud(contexto.getToken(), tienda, variante, null, claveAlias, ventas.clave(claveAlias));
    }

    @Cuando("registro una solicitud no satisfecha en la tienda {string} para una variante inexistente con cantidad {int} y clave {string}")
    public void registrarSolicitudVarianteInexistente(String tienda, int cantidad, String claveAlias) {
        ventas.registrarSolicitudVarianteInexistente(contexto.getToken(), tienda, cantidad, claveAlias,
                ventas.clave(claveAlias));
    }

    @Cuando("registro una solicitud no satisfecha en la tienda {string} para la variante {string} con cantidad {int} sin clave")
    public void registrarSolicitudSinClave(String tienda, String variante, int cantidad) {
        ventas.registrarSolicitud(contexto.getToken(), tienda, variante, cantidad, null, "");
    }

    @Cuando("anulo la solicitud no satisfecha {string} sin motivo")
    public void anularSolicitudSinMotivo(String solicitud) {
        ventas.anularSolicitud(contexto.getToken(), solicitud, "{}");
    }

    @Cuando("anulo la solicitud no satisfecha {string} con el motivo {string}")
    public void anularSolicitud(String solicitud, String motivo) {
        ventas.anularSolicitud(contexto.getToken(), solicitud, motivo(motivo));
    }

    @Cuando("anulo la solicitud no satisfecha {string} como {string} con el motivo {string}")
    public void anularSolicitudComo(String solicitud, String usuario, String motivo) {
        ventas.anularSolicitud(ventas.tokenDe(usuario), solicitud, motivo(motivo));
    }

    @Cuando("consulto la solicitud no satisfecha {string}")
    public void consultarSolicitud(String solicitud) {
        ventas.consultarSolicitud(contexto.getToken(), solicitud);
    }

    @Cuando("consulto la solicitud no satisfecha {string} como {string}")
    public void consultarSolicitudComo(String solicitud, String usuario) {
        ventas.consultarSolicitud(ventas.tokenDe(usuario), solicitud);
    }

    @Cuando("consulto las solicitudes no satisfechas de la tienda {string}")
    public void consultarSolicitudes(String tienda) {
        ventas.consultarSolicitudes(contexto.getToken(), Map.of("tiendaId", contexto.id("tienda:" + tienda)));
    }

    @Cuando("consulto las solicitudes no satisfechas de la tienda {string} con estado {string}")
    public void consultarSolicitudesConEstado(String tienda, String estado) {
        ventas.consultarSolicitudes(contexto.getToken(), Map.of(
                "tiendaId", contexto.id("tienda:" + tienda),
                "estado", estado));
    }

    @Cuando("consulto las ventanas de quiebre de la tienda {string} para la variante {string}")
    public void consultarVentanas(String tienda, String variante) {
        ventas.consultarVentanas(contexto.getToken(), tienda, variante);
    }

    @Cuando("consulto las ventanas de quiebre abiertas de la tienda {string}")
    public void consultarVentanasAbiertas(String tienda) {
        ventas.consultarVentanasAbiertas(contexto.getToken(), tienda);
    }

    @Cuando("consulto la ventana de quiebre por su identificador")
    public void consultarVentanaPorId() {
        ventas.consultarVentanaActual(contexto.getToken());
    }

    @Cuando("consulto la última ventana de quiebre como {string}")
    public void consultarVentanaComo(String usuario) {
        ventas.consultarVentanaActual(ventas.tokenDe(usuario));
    }

    @Cuando("recuerdo las ventanas de quiebre de la tienda {string} para la variante {string}")
    public void recordarVentanas(String tienda, String variante) {
        ventas.recordarVentanas(tienda, variante);
    }

    @Cuando("reconcilio las ventanas de quiebre de la tienda {string}")
    public void reconciliar(String tienda) {
        ventas.reconciliar(contexto.getToken(), tienda);
    }

    @Cuando("reconcilio las ventanas de quiebre de la tienda {string} como {string}")
    public void reconciliarComo(String tienda, String usuario) {
        ventas.reconciliar(ventas.tokenDe(usuario), tienda);
    }

    @Cuando("reconcilio las ventanas de quiebre sin tienda")
    public void reconciliarSinTienda() {
        ventas.reconciliarSinTienda(contexto.getToken());
    }

    @Entonces("el campo {string} es el identificador de la transacción {string}")
    public void campoEsTransaccion(String ruta, String alias) {
        contexto.getRespuesta().then().body(ruta, equalTo(ventas.transaccionId(alias)));
    }

    @Entonces("el campo {string} es el identificador de la línea de venta {string}")
    public void campoEsLinea(String ruta, String alias) {
        contexto.getRespuesta().then().body(ruta, equalTo(contexto.id("linea:" + alias)));
    }

    @Entonces("el campo {string} es el identificador del usuario {string}")
    public void campoEsUsuario(String ruta, String alias) {
        contexto.getRespuesta().then().body(ruta, equalTo(contexto.id("usuario:" + alias)));
    }

    @Entonces("el campo {string} es el identificador de la variante {string}")
    public void campoEsVariante(String ruta, String alias) {
        contexto.getRespuesta().then().body(ruta, equalTo(contexto.id("variante:" + alias)));
    }

    @Entonces("el campo {string} es el identificador de la solicitud no satisfecha {string}")
    public void campoEsSolicitud(String ruta, String alias) {
        contexto.getRespuesta().then().body(ruta, equalTo(contexto.id("solicitud:" + alias)));
    }

    @Entonces("el campo {string} es el identificador de la ventana de quiebre")
    public void campoEsVentana(String ruta) {
        contexto.getRespuesta().then().body(ruta, equalTo(contexto.id("ventana:id")));
    }

    @Entonces("el campo {string} es el número {string}")
    public void campoEsNumero(String ruta, String valor) {
        contexto.getRespuesta().then().body(ruta, equalTo(Float.parseFloat(valor)));
    }

    @Entonces("el campo {string} es verdadero")
    public void campoEsVerdadero(String ruta) {
        contexto.getRespuesta().then().body(ruta, equalTo(true));
    }

    @Entonces("el campo {string} es falso")
    public void campoEsFalso(String ruta) {
        contexto.getRespuesta().then().body(ruta, equalTo(false));
    }

    @Entonces("cada elemento del campo {string} es {string}")
    public void cadaElementoEs(String ruta, String valor) {
        contexto.getRespuesta().then().body(ruta, everyItem(equalTo(valor)));
    }

    @Entonces("todas las ventas son de la tienda {string}")
    public void todasLasVentasSonDeLaTienda(String tienda) {
        contexto.getRespuesta().then().body("content.tiendaId",
                everyItem(equalTo(contexto.id("tienda:" + tienda))));
    }

    @Entonces("todas las ventas tienen la transacción {string}")
    public void todasLasVentasTienenLaTransaccion(String transaccion) {
        contexto.getRespuesta().then().body("content.transaccionId",
                everyItem(equalTo(ventas.transaccionId(transaccion))));
    }

    @Entonces("la respuesta es un error de cliente")
    public void respuestaEsErrorDeCliente() {
        int codigo = contexto.getRespuesta().statusCode();
        assertThat(codigo).isBetween(400, 499);
    }

    @Entonces("las ventanas de quiebre de la tienda {string} para la variante {string} no cambiaron")
    public void ventanasSinCambios(String tienda, String variante) {
        assertThat(ventas.ventanasSinCambios(tienda, variante)).isTrue();
    }

    @Entonces("hay {int} ventanas de quiebre de la tienda {string} para la variante {string}")
    public void hayVentanas(int cantidad, String tienda, String variante) {
        assertThat(ventas.ventanas(tienda, variante)).hasSize(cantidad);
    }

    private String motivo(String motivo) {
        return "{\"motivo\": \"" + motivo + "\"}";
    }
}
