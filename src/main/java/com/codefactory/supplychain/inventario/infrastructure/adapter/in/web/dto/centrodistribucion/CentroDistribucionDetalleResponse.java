package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion;

public class CentroDistribucionDetalleResponse {

    private int id;
    private String nombre;
    private String ubicacion;
    private NodoResponse nodo;

    public CentroDistribucionDetalleResponse() {
    }

    public CentroDistribucionDetalleResponse(
            int id,
            String nombre,
            String ubicacion,
            NodoResponse nodo) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.nodo = nodo;
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

    public NodoResponse getNodo() {
        return nodo;
    }

    public void setNodo(NodoResponse nodo) {
        this.nodo = nodo;
    }
}