package com.plazoleta.trazabilidad.domain.usecase;

import com.plazoleta.trazabilidad.domain.api.ITraceServicePort;
import com.plazoleta.trazabilidad.domain.exception.OrderFromAnotherClientException;
import com.plazoleta.trazabilidad.domain.exception.RequiredFieldException;
import com.plazoleta.trazabilidad.domain.exception.TraceNotAllowedException;
import com.plazoleta.trazabilidad.domain.exception.TraceNotFoundException;
import com.plazoleta.trazabilidad.domain.model.Trace;
import com.plazoleta.trazabilidad.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.trazabilidad.domain.spi.ITracePersistencePort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class TraceUseCase implements ITraceServicePort {

    private static final String ROLE_CLIENT = "CLIENT";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private final ITracePersistencePort tracePersistencePort;
    private final IAuthenticatedUserPort authenticatedUserPort;
    private final Clock clock;

    public TraceUseCase(ITracePersistencePort tracePersistencePort, IAuthenticatedUserPort authenticatedUserPort,
                        Clock clock) {
        this.tracePersistencePort = tracePersistencePort;
        this.authenticatedUserPort = authenticatedUserPort;
        this.clock = clock;
    }

    @Override
    public Trace saveTrace(Trace trace) {
        validateRequiredFields(trace);
        validateAuthor(trace);

        trace.setId(null);
        // La fecha la pone este servicio en el momento del cambio, no quien llama.
        trace.setDate(LocalDateTime.now(clock));
        return tracePersistencePort.saveTrace(trace);
    }

    @Override
    public List<Trace> getOrderTraces(Long orderId) {
        if (orderId == null) {
            throw new RequiredFieldException("idPedido");
        }
        List<Trace> traces = tracePersistencePort.findByOrderId(orderId);
        if (traces.isEmpty()) {
            throw new TraceNotFoundException();
        }
        Long clientId = authenticatedUserPort.getAuthenticatedUserId();
        if (traces.stream().anyMatch(trace -> !clientId.equals(trace.getClientId()))) {
            throw new OrderFromAnotherClientException();
        }
        return traces;
    }

    private void validateRequiredFields(Trace trace) {
        if (trace.getOrderId() == null) {
            throw new RequiredFieldException("idPedido");
        }
        if (trace.getClientId() == null) {
            throw new RequiredFieldException("idCliente");
        }
        if (trace.getRestaurantId() == null) {
            throw new RequiredFieldException("idRestaurante");
        }
        if (trace.getNewStatus() == null) {
            throw new RequiredFieldException("estadoNuevo");
        }
    }

    /**
     * Quien registra el cambio es quien lo hizo: el cliente duenio del pedido (crear y cancelar) o un empleado
     * del restaurante del pedido (asignarse, listo y entregado).
     */
    private void validateAuthor(Trace trace) {
        String role = authenticatedUserPort.getAuthenticatedRole();
        boolean allowed = switch (role == null ? "" : role) {
            case ROLE_CLIENT -> authenticatedUserPort.getAuthenticatedUserId().equals(trace.getClientId());
            case ROLE_EMPLOYEE -> trace.getRestaurantId().equals(authenticatedUserPort.getAuthenticatedRestaurantId());
            default -> false;
        };
        if (!allowed) {
            throw new TraceNotAllowedException();
        }
    }
}
