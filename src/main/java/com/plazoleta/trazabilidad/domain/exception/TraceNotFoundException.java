package com.plazoleta.trazabilidad.domain.exception;

public class TraceNotFoundException extends DomainException {

    public TraceNotFoundException() {
        super("No hay registros de trazabilidad para el pedido indicado");
    }
}
