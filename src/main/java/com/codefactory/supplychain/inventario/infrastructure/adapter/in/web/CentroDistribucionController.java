package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import org.springframework.web.bind.annotation.RestController;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionResponse;
import jakarta.validation.Valid;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.application.service.CentroDistribucionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/centros-distribucion")
public class CentroDistribucionController {
    private final CentroDistribucionService centroDistribucionService;

    public CentroDistribucionController(CentroDistribucionService centroDistribucionService) {
        this.centroDistribucionService = centroDistribucionService;
    }

    @GetMapping("/{id}")
    public CentroDistribucionResponse obtenerCentroDistribucionPorId(@PathVariable int id) {
        CentroDistribucion centroDistribucion = centroDistribucionService.obtenerCentroDistribucionPorId(id);
        return new CentroDistribucionResponse(centroDistribucion.getId(), centroDistribucion.getNombre(), centroDistribucion.getUbicacion());
    }

    @GetMapping("/nombre/{nombre}")
    public CentroDistribucionResponse obtenerCentroDistribucionPorNombre(@PathVariable String nombre) {
        CentroDistribucion centroDistribucion = centroDistribucionService.obtenerCentroDistribucionPorNombre(nombre);
        return new CentroDistribucionResponse(centroDistribucion.getId(), centroDistribucion.getNombre(), centroDistribucion.getUbicacion());
    }
    
    @PostMapping
    public CentroDistribucionResponse crearCentroDistribucion(@Valid @RequestBody CentroDistribucionRequest centroDistribucionRequest) {
        CentroDistribucion centroDistribucion = centroDistribucionService.crearCentroDistribucion(centroDistribucionRequest.getNombre(), centroDistribucionRequest.getUbicacion());
        return new CentroDistribucionResponse(centroDistribucion.getId(), centroDistribucion.getNombre(), centroDistribucion.getUbicacion());
    }

    @PutMapping("/{id}")
    public CentroDistribucionResponse actualizarCentroDistribucion(@PathVariable int id, @Valid @RequestBody CentroDistribucionRequest centroDistribucionRequest) {
        CentroDistribucion centroDistribucion = centroDistribucionService.actualizarCentroDistribucion(id, centroDistribucionRequest.getNombre(), centroDistribucionRequest.getUbicacion());
        return new CentroDistribucionResponse(centroDistribucion.getId(), centroDistribucion.getNombre(), centroDistribucion.getUbicacion());
    }

    @DeleteMapping("/{id}")
    public void eliminarCentroDistribucion(@PathVariable int id) {
        centroDistribucionService.eliminarCentroDistribucion(id);
    }

}
