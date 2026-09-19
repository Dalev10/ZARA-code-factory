package com.codefactory.supplychain.inventario.application.dto;

public class CentroDistribucionConNodo {

    private int id;
    private String nombre;
    private String ubicacion;

    private Long nodoId;
    private String nodoTipo;
    private Long nodoCdId;
    private Long nodoTiendaId;

    public CentroDistribucionConNodo() {
    }

    public CentroDistribucionConNodo(
            int id,
            String nombre,
            String ubicacion,
            Long nodoId,
            String nodoTipo,
            Long nodoCdId,
            Long nodoTiendaId) {

        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.nodoId = nodoId;
        this.nodoTipo = nodoTipo;
        this.nodoCdId = nodoCdId;
        this.nodoTiendaId = nodoTiendaId;
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

    public Long getNodoId() {
        return nodoId;
    }

    public void setNodoId(Long nodoId) {
        this.nodoId = nodoId;
    }

    public String getNodoTipo() {
        return nodoTipo;
    }

    public void setNodoTipo(String nodoTipo) {
        this.nodoTipo = nodoTipo;
    }

    public Long getNodoCdId() {
        return nodoCdId;
    }

    public void setNodoCdId(Long nodoCdId) {
        this.nodoCdId = nodoCdId;
    }

    public Long getNodoTiendaId() {
        return nodoTiendaId;
    }

    public void setNodoTiendaId(Long nodoTiendaId) {
        this.nodoTiendaId = nodoTiendaId;
    }
}