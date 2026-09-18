package com.codefactory.supplychain.inventario.domain.exception.bodegatienda;

public class TiendaAsociadaNoExisteException extends RuntimeException {

    private TiendaAsociadaNoExisteException(String mensaje) {
        super(mensaje);
    }

    public static TiendaAsociadaNoExisteException porId(Long tiendaId) {
        return new TiendaAsociadaNoExisteException(
                "No existe una Tienda con id " + tiendaId + " para asociar a la Bodega_Tienda");
    }
}