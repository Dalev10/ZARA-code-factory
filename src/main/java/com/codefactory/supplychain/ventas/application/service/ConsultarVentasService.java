package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.in.ConsultarVentasUseCase;
import com.codefactory.supplychain.ventas.application.port.out.VentaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.VentaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.exception.VentaInvalidaException;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** HU-33: consulta historial sin revelar si existe una venta de otra tienda. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarVentasService implements ConsultarVentasUseCase {

    private final VentaRepositoryPort ventas;

    @Override
    public Page<Venta> consultar(UsuarioActual usuario, Filtros filtros, Pageable pageable) {
        if (filtros.desde() != null && filtros.hasta() != null && filtros.desde().isAfter(filtros.hasta())) {
            throw new VentaInvalidaException("El filtro desde no puede ser posterior a hasta");
        }
        if (AccesoTiendas.filtroFueraDeAlcance(usuario, filtros.tiendaId())) {
            return Page.empty(pageable);
        }
        return ventas.buscar(new VentaRepositoryPort.Filtro(AccesoTiendas.tiendasPermitidas(usuario),
                filtros.tiendaId(), filtros.varianteId(), filtros.transaccionId(), filtros.estado(),
                filtros.desde(), filtros.hasta()), pageable);
    }

    @Override
    public Venta obtener(UsuarioActual usuario, UUID ventaId) {
        return ventas.buscarPorId(ventaId)
                .filter(venta -> usuario.puedeOperarEn(venta.tiendaId()))
                .orElseThrow(() -> new VentaNoEncontradaException(ventaId));
    }
}
