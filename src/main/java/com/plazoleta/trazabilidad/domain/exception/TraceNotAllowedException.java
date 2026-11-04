package com.plazoleta.trazabilidad.domain.exception;

public class TraceNotAllowedException extends DomainException {

    public TraceNotAllowedException() {
        super("No puede registrar cambios de estado de un pedido que no le corresponde");
    }
}
