package com.plazoleta.trazabilidad.domain.spi;

import java.util.Optional;

/**
 * Puerto hacia el microservicio de plazoleta (restaurantes).
 */
public interface IRestaurantClientPort {

    /**
     * @return el id del propietario del restaurante, o vacio si el restaurante no existe.
     */
    Optional<Long> findRestaurantOwnerId(Long restaurantId);
}
