package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase.ResultadoReconciliacion;

public record ReconciliacionResponse(int paresProcesados, int creadas, int cerradas, int corregidas,
                                     int sinCambios, int sinRespaldoEnKardex) {

    public static ReconciliacionResponse desde(ResultadoReconciliacion r) {
        return new ReconciliacionResponse(r.paresProcesados(), r.creadas(), r.cerradas(), r.corregidas(),
                r.sinCambios(), r.sinRespaldoEnKardex());
    }
}
