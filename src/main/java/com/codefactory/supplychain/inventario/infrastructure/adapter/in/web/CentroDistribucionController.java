package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import org.springframework.web.bind.annotation.RestController;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionDTO;

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
    public CentroDistribucion obtenerCentroDistribucionPorId(@PathVariable int id) {
        return centroDistribucionService.obtenerCentroDistribucionPorId(id);
    }

    @GetMapping("/nombre/{nombre}")
    public CentroDistribucion obtenerCentroDistribucionPorNombre(@PathVariable String nombre) {
        return centroDistribucionService.obtenerCentroDistribucionPorNombre(nombre);
    }
    
    @PostMapping
    public CentroDistribucion crearCentroDistribucion(@Valid @RequestBody CentroDistribucionDTO centroDistribucionDTO) {
        return centroDistribucionService.crearCentroDistribucion(centroDistribucionDTO.getNombre(), centroDistribucionDTO.getUbicacion());
    }

    @PutMapping("/{id}")
    public CentroDistribucion actualizarCentroDistribucion(@PathVariable int id, @Valid @RequestBody CentroDistribucionDTO centroDistribucionDTO) {
        return centroDistribucionService.actualizarCentroDistribucion(id, centroDistribucionDTO.getNombre(), centroDistribucionDTO.getUbicacion());
    }

    @DeleteMapping("/{id}")
    public void eliminarCentroDistribucion(@PathVariable int id) {
        centroDistribucionService.eliminarCentroDistribucion(id);
    }

}
