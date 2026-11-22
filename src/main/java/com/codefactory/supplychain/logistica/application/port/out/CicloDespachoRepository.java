package com.codefactory.supplychain.logistica.application.port.out;

import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Persistencia de los ciclos de despacho. */
public interface CicloDespachoRepository {

    CicloDespacho guardar(CicloDespacho ciclo);

    Optional<CicloDespacho> buscarPorId(UUID cicloId);

    /** Mayor {@code numero_ciclo} de la orden; 0 si aún no tiene ciclos. */
    int ultimoNumeroCiclo(UUID ordenId);

    /** Del más reciente al más antiguo; el orden del {@code Pageable} no se aplica. */
    Page<CicloDespacho> listar(UUID ordenId, boolean sinViaje, Pageable pageable);
}
