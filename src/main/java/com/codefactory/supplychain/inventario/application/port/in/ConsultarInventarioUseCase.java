package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.application.dto.MovimientoKardexConsulta;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Consultas de inventario con el alcance del usuario (HU-30, D-33).
 */
public interface ConsultarInventarioUseCase {

    Page<Inventario> stockDeNodo(UUID nodoId, UsuarioActual usuario, Pageable pageable);

    List<Inventario> stockDeVariante(UUID varianteId, UsuarioActual usuario);

    Page<MovimientoKardexConsulta> kardex(ConsultaKardexComando comando, Pageable pageable);
}
