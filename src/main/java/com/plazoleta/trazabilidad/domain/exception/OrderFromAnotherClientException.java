package com.plazoleta.trazabilidad.domain.exception;

public class OrderFromAnotherClientException extends DomainException {

    public OrderFromAnotherClientException() {
        super("Solo puede consultar la trazabilidad de sus propios pedidos");
    }
}
