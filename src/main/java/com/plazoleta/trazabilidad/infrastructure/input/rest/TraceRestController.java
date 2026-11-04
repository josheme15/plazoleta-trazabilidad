package com.plazoleta.trazabilidad.infrastructure.input.rest;

import com.plazoleta.trazabilidad.application.dto.request.TraceRequestDto;
import com.plazoleta.trazabilidad.application.dto.response.TraceResponseDto;
import com.plazoleta.trazabilidad.application.handler.ITraceHandler;
import com.plazoleta.trazabilidad.infrastructure.exceptionhandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
@Tag(name = "Trazabilidad", description = "Historial de cambios de estado de los pedidos")
public class TraceRestController {

    private final ITraceHandler traceHandler;

    @Operation(summary = "Registrar un cambio de estado de un pedido",
            description = "Lo llama plazoleta-plazoleta en cada cambio de estado de un pedido, reenviando el token de "
                    + "quien hizo el cambio: el CLIENTE duenio del pedido (crear y cancelar) o un EMPLEADO del "
                    + "restaurante del pedido (asignarse, listo y entregado). La fecha la asigna este servicio.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cambio de estado registrado",
                    content = @Content(schema = @Schema(implementation = TraceResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes o estado invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no es CLIENTE ni EMPLEADO, o el pedido no le corresponde",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TraceResponseDto> saveTrace(@Valid @RequestBody TraceRequestDto traceRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(traceHandler.saveTrace(traceRequestDto));
    }

    @Operation(summary = "Consultar la trazabilidad de un pedido",
            description = "Devuelve el historial de cambios de estado de un pedido del cliente autenticado (sale de "
                    + "su token), del mas antiguo al mas reciente. Solo disponible para el rol CLIENTE y solo para "
                    + "sus propios pedidos.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial del pedido",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TraceResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "El id del pedido no es numerico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es CLIENTE o el pedido es de otro cliente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No hay registros para el pedido indicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<TraceResponseDto>> getOrderTraces(@PathVariable Long orderId) {
        return ResponseEntity.ok(traceHandler.getOrderTraces(orderId));
    }
}
