package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.centrodistribucion;

public class NodoResponse {

    private Long id;
    private String tipo;
    private Long cdId;
    private Long tiendaId;

    public NodoResponse() {
    }

    public NodoResponse(Long id, String tipo, Long cdId, Long tiendaId) {
        this.id = id;
        this.tipo = tipo;
        this.cdId = cdId;
        this.tiendaId = tiendaId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getCdId() {
        return cdId;
    }

    public void setCdId(Long cdId) {
        this.cdId = cdId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public void setTiendaId(Long tiendaId) {
        this.tiendaId = tiendaId;
    }
}