package com.plazoleta.trazabilidad.domain.usecase;

import com.plazoleta.trazabilidad.domain.api.IOrderEfficiencyServicePort;
import com.plazoleta.trazabilidad.domain.exception.NotRestaurantOwnerException;
import com.plazoleta.trazabilidad.domain.exception.RequiredFieldException;
import com.plazoleta.trazabilidad.domain.exception.RestaurantNotFoundException;
import com.plazoleta.trazabilidad.domain.model.EmployeeRanking;
import com.plazoleta.trazabilidad.domain.model.OrderEfficiency;
import com.plazoleta.trazabilidad.domain.model.OrderStatus;
import com.plazoleta.trazabilidad.domain.model.Trace;
import com.plazoleta.trazabilidad.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.trazabilidad.domain.spi.IRestaurantClientPort;
import com.plazoleta.trazabilidad.domain.spi.ITracePersistencePort;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * HU-18: el pedido "inicia" cuando el cliente lo hace (sin estado anterior -> PENDIENTE) y "termina" cuando se
 * entrega (-> ENTREGADO). Solo los pedidos entregados tienen inicio y fin, por eso son los que se miden.
 */
public class OrderEfficiencyUseCase implements IOrderEfficiencyServicePort {

    private static final double SECONDS_PER_MINUTE = 60.0;

    private final ITracePersistencePort tracePersistencePort;
    private final IAuthenticatedUserPort authenticatedUserPort;
    private final IRestaurantClientPort restaurantClientPort;
    // Zona horaria en la que se registraron las fechas (la del Clock con el que TraceUseCase las genera).
    private final Clock clock;

    public OrderEfficiencyUseCase(ITracePersistencePort tracePersistencePort,
                                  IAuthenticatedUserPort authenticatedUserPort,
                                  IRestaurantClientPort restaurantClientPort,
                                  Clock clock) {
        this.tracePersistencePort = tracePersistencePort;
        this.authenticatedUserPort = authenticatedUserPort;
        this.restaurantClientPort = restaurantClientPort;
        this.clock = clock;
    }

    @Override
    public List<OrderEfficiency> getRestaurantOrdersEfficiency(Long restaurantId) {
        validateOwner(restaurantId);
        return deliveredOrders(restaurantId);
    }

    @Override
    public List<EmployeeRanking> getEmployeeRanking(Long restaurantId) {
        validateOwner(restaurantId);
        Map<Long, List<OrderEfficiency>> byEmployee = deliveredOrders(restaurantId).stream()
                .filter(order -> order.employeeId() != null)
                .collect(Collectors.groupingBy(OrderEfficiency::employeeId));

        List<EmployeeStats> stats = byEmployee.entrySet().stream()
                .map(entry -> new EmployeeStats(entry.getKey(), entry.getValue().size(), entry.getValue().stream()
                        .mapToLong(OrderEfficiency::durationSeconds).average().orElse(0)))
                .sorted(Comparator.comparingDouble(EmployeeStats::averageSeconds)
                        .thenComparing(EmployeeStats::completedOrders, Comparator.reverseOrder())
                        .thenComparing(EmployeeStats::employeeId))
                .toList();

        List<EmployeeRanking> ranking = new ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            EmployeeStats s = stats.get(i);
            ranking.add(new EmployeeRanking(i + 1, s.employeeId(), s.completedOrders(), toMinutes(s.averageSeconds())));
        }
        return ranking;
    }

    private void validateOwner(Long restaurantId) {
        if (restaurantId == null) {
            throw new RequiredFieldException("idRestaurante");
        }
        Long ownerId = restaurantClientPort.findRestaurantOwnerId(restaurantId)
                .orElseThrow(RestaurantNotFoundException::new);
        if (!ownerId.equals(authenticatedUserPort.getAuthenticatedUserId())) {
            throw new NotRestaurantOwnerException();
        }
    }

    private List<OrderEfficiency> deliveredOrders(Long restaurantId) {
        // Los registros llegan ordenados por fecha; se agrupan por pedido conservando ese orden.
        Map<Long, List<Trace>> byOrder = tracePersistencePort.findByRestaurantId(restaurantId).stream()
                .collect(Collectors.groupingBy(Trace::getOrderId, LinkedHashMap::new, Collectors.toList()));
        return byOrder.entrySet().stream()
                .map(entry -> toEfficiency(entry.getKey(), entry.getValue()))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(OrderEfficiency::startDate).thenComparing(OrderEfficiency::orderId))
                .toList();
    }

    private Optional<OrderEfficiency> toEfficiency(Long orderId, List<Trace> traces) {
        Optional<Trace> start = traces.stream()
                .filter(t -> t.getPreviousStatus() == null && t.getNewStatus() == OrderStatus.PENDIENTE)
                .findFirst();
        Optional<Trace> end = traces.stream().filter(t -> t.getNewStatus() == OrderStatus.ENTREGADO).findFirst();
        if (start.isEmpty() || end.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime startDate = start.get().getDate();
        LocalDateTime endDate = end.get().getDate();
        // Se calcula con fechas con zona horaria para que un cambio de horario no altere la duracion.
        long seconds = Duration.between(startDate.atZone(clock.getZone()), endDate.atZone(clock.getZone()))
                .toSeconds();
        // El empleado que atendio el pedido es el que figura al entregarlo (el asignado).
        Long employeeId = end.get().getEmployeeId() != null ? end.get().getEmployeeId() : lastEmployee(traces);
        return Optional.of(new OrderEfficiency(orderId, employeeId, startDate, endDate, seconds, toMinutes(seconds)));
    }

    private static Long lastEmployee(List<Trace> traces) {
        Long employeeId = null;
        for (Trace trace : traces) {
            if (trace.getEmployeeId() != null) {
                employeeId = trace.getEmployeeId();
            }
        }
        return employeeId;
    }

    /**
     * Minutos con 2 decimales.
     */
    private static double toMinutes(double seconds) {
        return Math.round(seconds / SECONDS_PER_MINUTE * 100) / 100.0;
    }

    private record EmployeeStats(Long employeeId, int completedOrders, double averageSeconds) {
    }
}
