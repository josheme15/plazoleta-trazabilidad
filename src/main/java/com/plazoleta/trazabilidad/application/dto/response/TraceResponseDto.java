package com.plazoleta.trazabilidad.application.dto.response;

import com.plazoleta.trazabilidad.domain.model.OrderStatus;
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
@Schema(description = "Registro de un cambio de estado de un pedido")
public class TraceResponseDto {

    @Schema(example = "6727a1f0c2b14b3a5c9d8e01")
    private String id;

    @Schema(example = "10")
    private Long orderId;

    @Schema(example = "30")
    private Long clientId;

    @Schema(example = "1")
    private Long restaurantId;

    @Schema(example = "8")
    private Long employeeId;

    @Schema(example = "PENDIENTE")
    private OrderStatus previousStatus;

    @Schema(example = "EN_PREPARACION")
    private OrderStatus newStatus;

    @Schema(example = "2026-11-04T10:15:30")
    private LocalDateTime date;
}
