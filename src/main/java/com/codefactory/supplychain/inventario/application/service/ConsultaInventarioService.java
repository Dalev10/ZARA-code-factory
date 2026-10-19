package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.dto.MovimientoKardexConsulta;
import com.codefactory.supplychain.inventario.application.port.in.ConsultaKardexComando;
import com.codefactory.supplychain.inventario.application.port.in.ConsultarInventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de inventario de solo lectura (HU-39).
 *
 * El alcance por tienda se aplica aquí a partir de {@link UsuarioActual}: en
 * la variante, el filtro viaja a la consulta SQL y no se trae todo para
 * descartar después.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaInventarioService implements ConsultarInventarioUseCase {

    private static final Instant DESDE_SIN_LIMITE = Instant.EPOCH;
    private static final Instant HASTA_SIN_LIMITE = Instant.parse("9999-12-31T23:59:59Z");

    private final AccesoNodoInventario accesoNodoInventario;
    private final InventarioRepositoryPort inventarioRepositoryPort;
    private final MovimientoInventarioRepositoryPort movimientoRepositoryPort;

    @Override
    public Page<Inventario> stockDeNodo(UUID nodoId, UsuarioActual usuario, Pageable pageable) {
        accesoNodoInventario.exigirVisibilidad(nodoId, usuario);
        return inventarioRepositoryPort.buscarPorNodo(nodoId, pageable);
    }

    @Override
    public List<Inventario> stockDeVariante(UUID varianteId, UsuarioActual usuario) {
        if (usuario.tieneScope(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS)) {
            return inventarioRepositoryPort.buscarPorVariante(varianteId);
        }
        List<Inventario> visibles = new ArrayList<>();
        if (!usuario.tiendasPermitidas().isEmpty()) {
            visibles.addAll(inventarioRepositoryPort.buscarPorVarianteEnTiendas(varianteId, usuario.tiendasPermitidas()));
        }
        if (AccesoNodoInventario.puedeVerCds(usuario)) {
            visibles.addAll(inventarioRepositoryPort.buscarPorVarianteEnCds(varianteId));
        }
        return visibles;
    }

    @Override
    public Page<MovimientoKardexConsulta> kardex(ConsultaKardexComando comando, Pageable pageable) {
        accesoNodoInventario.exigirVisibilidad(comando.nodoId(), comando.usuario());
        Instant desde = comando.desde() != null ? comando.desde() : DESDE_SIN_LIMITE;
        Instant hasta = comando.hasta() != null ? comando.hasta() : HASTA_SIN_LIMITE;
        return movimientoRepositoryPort.buscarKardex(comando.nodoId(), comando.tipo(), desde, hasta, pageable);
    }
}
