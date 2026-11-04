package com.plazoleta.trazabilidad.application.dto.request;

import com.plazoleta.trazabilidad.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cambio de estado de un pedido")
public class TraceRequestDto {

    @Schema(example = "10")
    @NotNull(message = "El id del pedido es obligatorio")
    private Long orderId;

    @Schema(example = "30")
    @NotNull(message = "El id del cliente es obligatorio")
    private Long clientId;

    @Schema(example = "1")
    @NotNull(message = "El id del restaurante es obligatorio")
    private Long restaurantId;

    @Schema(example = "8", description = "Empleado asignado al pedido (null si aun no tiene)")
    private Long employeeId;

    @Schema(example = "PENDIENTE", description = "Estado anterior (null cuando el pedido se crea)")
    private OrderStatus previousStatus;

    @Schema(example = "EN_PREPARACION")
    @NotNull(message = "El estado nuevo es obligatorio")
    private OrderStatus newStatus;
}
