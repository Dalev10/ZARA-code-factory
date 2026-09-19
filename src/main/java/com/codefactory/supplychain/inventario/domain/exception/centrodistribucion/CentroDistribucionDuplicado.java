package com.codefactory.supplychain.inventario.domain.exception.centrodistribucion;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

public class CentroDistribucionDuplicado extends ReglaDeNegocioException {
    public CentroDistribucionDuplicado(String message) {
        super(message);
    }
}

