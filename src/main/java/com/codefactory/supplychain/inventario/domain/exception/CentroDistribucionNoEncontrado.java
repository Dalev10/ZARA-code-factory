package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

public class CentroDistribucionNoEncontrado extends ReglaDeNegocioException {

    public CentroDistribucionNoEncontrado(String message) {
        super(message);
    }
    
}
