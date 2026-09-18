package com.codefactory.supplychain.inventario.application.service.bodegatienda;

import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.ConsultarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.EliminarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.ModificarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.RegistrarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.ListarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.BodegaTiendaRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.InventarioPorNodoPort;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.TiendaConsultaPort;
import com.codefactory.supplychain.inventario.domain.exception.bodegatienda.BodegaTiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.bodegatienda.BodegaTiendaInvalidaException;
import com.codefactory.supplychain.inventario.domain.exception.bodegatienda.BodegaTiendaYaExisteException;
import com.codefactory.supplychain.inventario.domain.exception.bodegatienda.TiendaAsociadaNoExisteException;
import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

/**
 * Servicio de aplicación del subdominio {@code BodegaTienda}.
 *
 * Orquesta los casos de uso de registro, consulta, modificación y
 * eliminación mediante los puertos de entrada y salida de la arquitectura
 * hexagonal. La validación de las invariantes de la entidad se delega a
 * {@link BodegaTienda} y la existencia de la tienda asociada se consulta a
 * través de {@link TiendaConsultaPort}.
 */
public class BodegaTiendaService implements
        RegistrarBodegaTiendaUseCase,
        ConsultarBodegaTiendaUseCase,
        ModificarBodegaTiendaUseCase,
        EliminarBodegaTiendaUseCase,
        ListarBodegaTiendaUseCase {

    private final BodegaTiendaRepositoryPort bodegaTiendaRepositoryPort;
    private final TiendaConsultaPort tiendaConsultaPort;
    private final InventarioPorNodoPort inventarioPorNodoPort;

    /**
     * Crea el servicio con sus dependencias de aplicación.
     *
     * @param bodegaTiendaRepositoryPort puerto de salida para persistir y
     *                                    consultar bodegas de tienda
     * @param tiendaConsultaPort puerto de salida para comprobar y consultar la
     *                           tienda asociada
     * @param inventarioPorNodoPort puerto de salida para consultar el inventario
     *                              asociado al nodo
     */
    public BodegaTiendaService(BodegaTiendaRepositoryPort bodegaTiendaRepositoryPort,
            TiendaConsultaPort tiendaConsultaPort,
            InventarioPorNodoPort inventarioPorNodoPort) {

        this.bodegaTiendaRepositoryPort = bodegaTiendaRepositoryPort;
        this.tiendaConsultaPort = tiendaConsultaPort;
        this.inventarioPorNodoPort = inventarioPorNodoPort;
    }

    /**
     * Registra una bodega para una tienda existente.
     *
     * @param tiendaId identificador de la tienda a la que se asociará la bodega
     * @return la bodega de tienda registrada
     * @throws TiendaAsociadaNoExisteException si la tienda no existe
     * @throws BodegaTiendaYaExisteException si ya existe una bodega para la
     *                                       tienda indicada
     * @throws BodegaTiendaInvalidaException si el identificador de la tienda es
     *                                       inválido
     */
    public BodegaTienda registrar(Long tiendaId) {
        if (!tiendaConsultaPort.existe(tiendaId)) {
            throw TiendaAsociadaNoExisteException.porId(tiendaId);
        }

        if (bodegaTiendaRepositoryPort.existePorTiendaId(tiendaId)) {
            throw BodegaTiendaYaExisteException.porTiendaId(tiendaId);
        }

        BodegaTienda nuevaBodega = BodegaTienda.crear(tiendaId);
        return bodegaTiendaRepositoryPort.guardar(nuevaBodega);
    }

    /**
     * Consulta una bodega de tienda por su identificador.
     *
     * @param id identificador de la bodega de tienda
     * @return la bodega encontrada
     * @throws BodegaTiendaNoEncontradaException si no existe una bodega con el
     *                                           identificador indicado
     */
    public BodegaTiendaConsulta consultarPorId(Long id) {
        BodegaTienda bodegaTienda = bodegaTiendaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> BodegaTiendaNoEncontradaException.porId(id));
        return enriquecer(bodegaTienda);
    }

    /**
     * Consulta una bodega de tienda por el identificador de su tienda asociada.
     *
     * @param tiendaId identificador de la tienda asociada
     * @return la bodega encontrada
     * @throws BodegaTiendaNoEncontradaException si no existe una bodega asociada
     *                                           a la tienda indicada
     */
    public BodegaTiendaConsulta consultarPorTiendaId(Long tiendaId) {
        BodegaTienda bodegaTienda = bodegaTiendaRepositoryPort.buscarPorTiendaId(tiendaId)
                .orElseThrow(() -> BodegaTiendaNoEncontradaException.porTiendaId(tiendaId));
        return enriquecer(bodegaTienda);
    }

    /**
     * Modifica la tienda asociada a una bodega existente.
     *
     * @param id identificador de la bodega que se modificará
     * @param bodegaTienda datos de la bodega con la nueva tienda asociada
     * @return la bodega modificada
     * @throws BodegaTiendaNoEncontradaException si no existe la bodega indicada
     * @throws TiendaAsociadaNoExisteException si la nueva tienda no existe
     * @throws BodegaTiendaYaExisteException si la nueva tienda ya tiene una
     *                                       bodega asociada
     * @throws BodegaTiendaInvalidaException si los datos de la bodega son
     *                                       inválidos
     */
    public BodegaTienda modificar(Long id, BodegaTienda bodegaTienda) {
        BodegaTienda actual = bodegaTiendaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> BodegaTiendaNoEncontradaException.porId(id));
        Long nuevaTiendaId = bodegaTienda.getTiendaId();

        if (!actual.getTiendaId().equals(nuevaTiendaId)) {
            if (!tiendaConsultaPort.existe(nuevaTiendaId)) {
                throw TiendaAsociadaNoExisteException.porId(nuevaTiendaId);
            }
            if (bodegaTiendaRepositoryPort.existePorTiendaId(nuevaTiendaId)) {
                throw BodegaTiendaYaExisteException.porTiendaId(nuevaTiendaId);
            }
        }

        BodegaTienda actualizada = BodegaTienda.reconstruir(id, nuevaTiendaId);
        return bodegaTiendaRepositoryPort.guardar(actualizada);
    }

    /**
     * Elimina una bodega de tienda existente.
     *
     * @param id identificador de la bodega que se eliminará
     * @throws BodegaTiendaNoEncontradaException si no existe una bodega con el
     *                                           identificador indicado
     */
    public void eliminar(Long id) {
        if (!bodegaTiendaRepositoryPort.existePorId(id)) {
            throw BodegaTiendaNoEncontradaException.porId(id);
        }
        // TODO HU-10: validar inventario y movimientos asociados cuando exista
        // el historial necesario; por ahora se conserva el borrado físico simple.
        bodegaTiendaRepositoryPort.eliminar(id);
    }

    @Override
    public java.util.List<BodegaTienda> listarTodas() {
        return bodegaTiendaRepositoryPort.buscarTodas();
    }

    private BodegaTiendaConsulta enriquecer(BodegaTienda bodegaTienda) {
        return new BodegaTiendaConsulta(
                bodegaTienda,
                tiendaConsultaPort.buscarInfo(bodegaTienda.getTiendaId()).orElse(null),
                inventarioPorNodoPort.consultarPorNodo(bodegaTienda.getId()));
    }

}
