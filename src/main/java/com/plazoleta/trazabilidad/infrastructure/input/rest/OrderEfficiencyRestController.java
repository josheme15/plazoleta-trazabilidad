package com.plazoleta.trazabilidad.infrastructure.input.rest;

import com.plazoleta.trazabilidad.application.dto.response.EmployeeRankingResponseDto;
import com.plazoleta.trazabilidad.application.dto.response.OrderEfficiencyResponseDto;
import com.plazoleta.trazabilidad.application.handler.IOrderEfficiencyHandler;
import com.plazoleta.trazabilidad.infrastructure.exceptionhandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/traces/restaurants/{restaurantId}")
@RequiredArgsConstructor
@Tag(name = "Eficiencia", description = "Tiempos de los pedidos de un restaurante (solo su propietario)")
public class OrderEfficiencyRestController {

    private final IOrderEfficiencyHandler orderEfficiencyHandler;

    @Operation(summary = "Tiempo de inicio y fin de cada pedido",
            description = "Devuelve, para cada pedido entregado del restaurante, cuando inicio (el cliente lo hizo, "
                    + "PENDIENTE), cuando termino (se entrego, ENTREGADO) y su duracion; del mas antiguo al mas "
                    + "reciente. Los pedidos en proceso o cancelados no tienen fin y no se incluyen. Solo disponible "
                    + "para el PROPIETARIO del restaurante.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eficiencia de los pedidos del restaurante",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = OrderEfficiencyResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "El id del restaurante no es numerico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no es PROPIETARIO o no es el propietario de este restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "El servicio de plazoleta no esta disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/orders")
    public ResponseEntity<List<OrderEfficiencyResponseDto>> getRestaurantOrdersEfficiency(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderEfficiencyHandler.getRestaurantOrdersEfficiency(restaurantId));
    }

    @Operation(summary = "Ranking de empleados por tiempo medio de sus pedidos",
            description = "Ranking de los empleados del restaurante segun el tiempo medio entre que sus pedidos "
                    + "inician y se terminan (entregados), del mas rapido (posicion 1) al mas lento. Solo disponible "
                    + "para el PROPIETARIO del restaurante.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking de empleados",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = EmployeeRankingResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "El id del restaurante no es numerico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no es PROPIETARIO o no es el propietario de este restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "El servicio de plazoleta no esta disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/employees/ranking")
    public ResponseEntity<List<EmployeeRankingResponseDto>> getEmployeeRanking(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderEfficiencyHandler.getEmployeeRanking(restaurantId));
    }
}
