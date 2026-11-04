package com.plazoleta.trazabilidad.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Posicion de un empleado en el ranking de tiempo medio de sus pedidos")
public class EmployeeRankingResponseDto {

    @Schema(example = "1", description = "1 = el empleado mas rapido")
    private int position;

    @Schema(example = "8")
    private Long employeeId;

    @Schema(example = "12", description = "Pedidos entregados")
    private int completedOrders;

    @Schema(example = "18.75", description = "Tiempo medio entre que un pedido inicia y se termina, en minutos")
    private double averageDurationMinutes;
}
