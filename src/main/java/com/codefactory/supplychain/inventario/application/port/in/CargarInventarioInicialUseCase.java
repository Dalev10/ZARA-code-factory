package com.codefactory.supplychain.inventario.application.port.in;

/**
 * Carga inicial en lote de un nodo: todo o nada, solo filas nuevas (D-28, D-30).
 */
public interface CargarInventarioInicialUseCase {

    ResultadoCargaInicial cargar(CargaInicialComando comando);
}
