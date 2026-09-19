package com.codefactory.supplychain.inventario.application.port.in.bodegatienda;

import java.util.List;

import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.InventarioPorNodoPort.InventarioResumen;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.TiendaConsultaPort.TiendaInfo;
import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;

public record BodegaTiendaConsulta(
        BodegaTienda bodegaTienda,
        TiendaInfo tienda,
        List<InventarioResumen> inventario) {
}
