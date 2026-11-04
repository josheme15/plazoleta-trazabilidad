package com.plazoleta.trazabilidad.domain.spi;

import com.plazoleta.trazabilidad.domain.model.Trace;

import java.util.List;

public interface ITracePersistencePort {

    Trace saveTrace(Trace trace);

    /**
     * @return los registros del pedido ordenados por fecha ascendente.
     */
    List<Trace> findByOrderId(Long orderId);
}
