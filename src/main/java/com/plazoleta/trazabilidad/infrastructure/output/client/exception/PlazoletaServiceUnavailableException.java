package com.plazoleta.trazabilidad.infrastructure.output.client.exception;

public class PlazoletaServiceUnavailableException extends RuntimeException {

    public PlazoletaServiceUnavailableException(Throwable cause) {
        super("No fue posible validar el restaurante: el servicio de plazoleta no esta disponible", cause);
    }
}
