package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

import java.util.UUID;

public class TransaccionVentaReutilizadaException extends RecursoDuplicadoException {

    public TransaccionVentaReutilizadaException(UUID transaccionId) {
        super("La transacción " + transaccionId + " ya se usó con datos diferentes");
    }
}
