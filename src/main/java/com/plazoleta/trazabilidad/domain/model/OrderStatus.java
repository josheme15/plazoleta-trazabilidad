package com.plazoleta.trazabilidad.domain.model;

/**
 * Estados del pedido (los mismos de plazoleta-plazoleta).
 */
public enum OrderStatus {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO
}
