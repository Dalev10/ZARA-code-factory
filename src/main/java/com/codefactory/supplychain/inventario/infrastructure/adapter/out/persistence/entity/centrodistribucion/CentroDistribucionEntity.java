package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.centrodistribucion;

import jakarta.persistence.*;

@Entity
@Table(name = "cd")
public class CentroDistribucionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id")
    private Integer id;

    @Column (name = "nombre", nullable = false)
    private String nombre;

    @Column (name = "ubicacion")
    private String ubicacion;

    public CentroDistribucionEntity() {
    }

    public CentroDistribucionEntity(Integer id, String nombre, String ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
    }

    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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
