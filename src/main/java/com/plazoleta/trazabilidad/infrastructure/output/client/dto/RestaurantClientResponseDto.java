package com.plazoleta.trazabilidad.infrastructure.output.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de GET /restaurants/{id} del microservicio de plazoleta. Solo se mapean los campos necesarios.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantClientResponseDto(Long id, Long ownerId) {
}
