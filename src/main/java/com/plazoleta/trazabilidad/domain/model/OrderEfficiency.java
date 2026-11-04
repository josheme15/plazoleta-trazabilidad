package com.plazoleta.trazabilidad.domain.model;

import java.time.LocalDateTime;

/**
 * Tiempo de un pedido entregado: desde que el cliente lo hizo (PENDIENTE) hasta que se entrego (ENTREGADO).
 */
public record OrderEfficiency(Long orderId, Long employeeId, LocalDateTime startDate, LocalDateTime endDate,
                              long durationSeconds, double durationMinutes) {
}
