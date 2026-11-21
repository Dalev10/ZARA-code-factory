package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.dto.ExistenciaConsulta;
import com.codefactory.supplychain.inventario.application.dto.MovimientoKardexConsulta;
import com.codefactory.supplychain.inventario.application.port.in.ConsultaKardexComando;
import com.codefactory.supplychain.inventario.application.port.in.ConsultarInventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.VariantesCatalogoPort;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final VariantesCatalogoPort variantesCatalogoPort;

    @Override
    public Page<ExistenciaConsulta> stockDeNodo(UUID nodoId, UsuarioActual usuario, Pageable pageable) {
        accesoNodoInventario.exigirVisibilidad(nodoId, usuario);
        Page<Inventario> pagina = inventarioRepositoryPort.buscarPorNodo(nodoId, pageable);
        return new PageImpl<>(conSku(pagina.getContent()), pagina.getPageable(), pagina.getTotalElements());
    }

    @Override
    public List<ExistenciaConsulta> stockDeVariante(UUID varianteId, UsuarioActual usuario) {
        if (usuario.tieneScope(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS)) {
            return conSku(inventarioRepositoryPort.buscarPorVariante(varianteId));
        }
        List<Inventario> visibles = new ArrayList<>();
        if (!usuario.tiendasPermitidas().isEmpty()) {
            visibles.addAll(inventarioRepositoryPort.buscarPorVarianteEnTiendas(varianteId, usuario.tiendasPermitidas()));
        }
        if (AccesoNodoInventario.puedeVerCds(usuario)) {
            visibles.addAll(inventarioRepositoryPort.buscarPorVarianteEnCds(varianteId));
        }
        return conSku(visibles);
    }

    @Override
    public Page<MovimientoKardexConsulta> kardex(ConsultaKardexComando comando, Pageable pageable) {
        accesoNodoInventario.exigirVisibilidad(comando.nodoId(), comando.usuario());
        Instant desde = comando.desde() != null ? comando.desde() : DESDE_SIN_LIMITE;
        Instant hasta = comando.hasta() != null ? comando.hasta() : HASTA_SIN_LIMITE;
        return movimientoRepositoryPort.buscarKardex(comando.nodoId(), comando.tipo(), desde, hasta, pageable);
    }

    /** Una sola consulta a catalogo por página o lista, no una por fila. */
    private List<ExistenciaConsulta> conSku(List<Inventario> existencias) {
        if (existencias.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> skus = variantesCatalogoPort.skusPorId(
                existencias.stream().map(Inventario::getVarianteId).collect(Collectors.toSet()));
        return existencias.stream()
                .map(existencia -> new ExistenciaConsulta(existencia, skus.get(existencia.getVarianteId())))
                .toList();
    }
}
