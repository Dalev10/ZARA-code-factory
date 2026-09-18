package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public class CentroDistribucionRequest {

    @NotBlank(message = "El nombre del centro de distribución no puede estar vacío")
    @Size(max = 150, message = "El nombre del centro de distribución no puede tener más de 150 caracteres")
    private String nombre;

    @Size(max = 255, message = "La ubicación del centro de distribución no puede tener más de 255 caracteres")
    private String ubicacion;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}
