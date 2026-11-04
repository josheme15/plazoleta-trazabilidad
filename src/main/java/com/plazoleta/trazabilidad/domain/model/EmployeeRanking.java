package com.plazoleta.trazabilidad.domain.model;

/**
 * Posicion de un empleado en el ranking de tiempo medio de sus pedidos (1 = el mas rapido).
 */
public record EmployeeRanking(int position, Long employeeId, int completedOrders, double averageDurationMinutes) {
}
