package com.codefactory.supplychain.inventario.application.port.in.bodegatienda;

import java.util.List;

import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

public interface ListarBodegaTiendaUseCase {

    List<BodegaTienda> listarTodas();
}
