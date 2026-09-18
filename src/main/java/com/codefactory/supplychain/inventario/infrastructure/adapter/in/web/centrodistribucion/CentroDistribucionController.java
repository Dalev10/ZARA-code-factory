package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.centrodistribucion;

import org.springframework.web.bind.annotation.RestController;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion.CentroDistribucionRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion.CentroDistribucionResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.centrodistribucion.CentroDistribucionWebMapper;
import jakarta.validation.Valid;
import com.codefactory.supplychain.inventario.domain.model.centrodistribucion.CentroDistribucion;
import com.codefactory.supplychain.inventario.application.service.centrodistribucion.CentroDistribucionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/centros-distribucion")
public class CentroDistribucionController {
    private final CentroDistribucionService centroDistribucionService;
    private final CentroDistribucionWebMapper centroDistribucionWebMapper;

    public CentroDistribucionController(CentroDistribucionService centroDistribucionService, CentroDistribucionWebMapper centroDistribucionWebMapper) {
        this.centroDistribucionService = centroDistribucionService;
        this.centroDistribucionWebMapper = centroDistribucionWebMapper;
    }

    @GetMapping("/{id}")
    public CentroDistribucionResponse obtenerCentroDistribucionPorId(@PathVariable int id) {
        CentroDistribucion centroDistribucion = centroDistribucionService.obtenerCentroDistribucionPorId(id);
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
    }

    @GetMapping("/nombre/{nombre}")
    public CentroDistribucionResponse obtenerCentroDistribucionPorNombre(@PathVariable String nombre) {
        CentroDistribucion centroDistribucion = centroDistribucionService.obtenerCentroDistribucionPorNombre(nombre);
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
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
