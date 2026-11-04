package com.plazoleta.trazabilidad.domain.spi;

/**
 * Datos del usuario autenticado (salen del JWT).
 */
public interface IAuthenticatedUserPort {

    Long getAuthenticatedUserId();

    String getAuthenticatedRole();

    /**
     * @return el restaurante del empleado, o null si el usuario no es empleado.
     */
    Long getAuthenticatedRestaurantId();
}
