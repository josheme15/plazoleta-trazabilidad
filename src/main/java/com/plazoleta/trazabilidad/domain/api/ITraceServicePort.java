package com.plazoleta.trazabilidad.domain.api;

import com.plazoleta.trazabilidad.domain.model.Trace;

import java.util.List;

public interface ITraceServicePort {

    /**
     * Registra un cambio de estado de un pedido. Lo llama plazoleta con el token de quien hizo el cambio.
     */
    Trace saveTrace(Trace trace);

    /**
     * Historial de cambios de estado de un pedido del cliente autenticado, del mas antiguo al mas reciente.
     */
    List<Trace> getOrderTraces(Long orderId);
}
