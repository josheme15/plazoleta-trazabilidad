package com.plazoleta.trazabilidad.infrastructure.security;

/**
 * Datos del usuario autenticado extraidos del JWT (claims "id", "role" y, para empleados, "restaurantId").
 */
public record AuthenticatedUser(Long id, String role, Long restaurantId) {
}
