package com.plazoleta.trazabilidad.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tiempo de un pedido entregado: desde que el cliente lo hizo hasta que se entrego")
public class OrderEfficiencyResponseDto {

    @Schema(example = "10")
    private Long orderId;

    @Schema(example = "8", description = "Empleado que atendio el pedido")
    private Long employeeId;

    @Schema(example = "2026-11-04T10:00:00", description = "Inicio: el cliente hizo el pedido (PENDIENTE)")
    private LocalDateTime startDate;

    @Schema(example = "2026-11-04T10:25:30", description = "Fin: el pedido se entrego (ENTREGADO)")
    private LocalDateTime endDate;

    @Schema(example = "1530")
    private long durationSeconds;

    @Schema(example = "25.5")
    private double durationMinutes;
}
