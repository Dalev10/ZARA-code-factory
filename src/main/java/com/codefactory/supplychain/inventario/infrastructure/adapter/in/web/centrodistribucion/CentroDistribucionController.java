package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.centrodistribucion;

import org.springframework.web.bind.annotation.RestController;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion.CentroDistribucionRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion.CentroDistribucionResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.centrodistribucion.CentroDistribucionWebMapper;
import jakarta.validation.Valid;
import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import com.codefactory.supplychain.inventario.application.service.centrodistribucion.CentroDistribucionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/centros-distribucion")
public class CentroDistribucionController {
    private final CentroDistribucionService centroDistribucionService;
    private final CentroDistribucionWebMapper centroDistribucionWebMapper;

    public CentroDistribucionController(CentroDistribucionService centroDistribucionService, CentroDistribucionWebMapper centroDistribucionWebMapper) {
        this.centroDistribucionService = centroDistribucionService;
        this.centroDistribucionWebMapper = centroDistribucionWebMapper;
    }

    @GetMapping
    public List<CentroDistribucionResponse> obtenerCentroDistribucion(@RequestParam(required = false) Integer id, @RequestParam(required = false) String nombre, @RequestParam(required = false) String ubicacion) {
        return centroDistribucionService
            .buscarCentrosDistribucion(id, nombre, ubicacion)
            .stream()
            .map(centroDistribucionWebMapper::toResponse)
            .toList();
    }
    
    @PostMapping
    public CentroDistribucionResponse crearCentroDistribucion(@Valid @RequestBody CentroDistribucionRequest centroDistribucionRequest) {
        CentroDistribucion centroDistribucion = centroDistribucionService.crearCentroDistribucion(centroDistribucionRequest.getNombre(), centroDistribucionRequest.getUbicacion());
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
    }

    @PutMapping("/{id}")
    public CentroDistribucionResponse actualizarCentroDistribucion(@PathVariable int id, @Valid @RequestBody CentroDistribucionRequest centroDistribucionRequest) {
        CentroDistribucion centroDistribucion = centroDistribucionService.actualizarCentroDistribucion(id, centroDistribucionRequest.getNombre(), centroDistribucionRequest.getUbicacion());
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
    }

    @DeleteMapping("/{id}")
    public void eliminarCentroDistribucion(@PathVariable int id) {
        centroDistribucionService.eliminarCentroDistribucion(id);
    }

}
