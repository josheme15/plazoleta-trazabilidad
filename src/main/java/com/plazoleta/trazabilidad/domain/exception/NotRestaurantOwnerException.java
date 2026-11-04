package com.plazoleta.trazabilidad.domain.exception;

public class NotRestaurantOwnerException extends DomainException {

    public NotRestaurantOwnerException() {
        super("Solo el propietario del restaurante puede consultar la eficiencia de sus pedidos");
    }
}
