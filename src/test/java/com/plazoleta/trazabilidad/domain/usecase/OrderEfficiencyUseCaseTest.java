package com.plazoleta.trazabilidad.domain.usecase;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEfficiencyUseCaseTest {

    private static final Long RESTAURANT_ID = 1L;
    private static final Long OWNER_ID = 2L;
    private static final Long CLIENT_ID = 30L;
    private static final Long EMPLOYEE_A = 8L;
    private static final Long EMPLOYEE_B = 9L;
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 4, 10, 0, 0);

    @Mock
    private ITracePersistencePort tracePersistencePort;

    @Mock
    private IAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private IRestaurantClientPort restaurantClientPort;

    @InjectMocks
    private OrderEfficiencyUseCase useCase;

    private final List<Trace> traces = new ArrayList<>();

    private void trace(Long orderId, Long employeeId, OrderStatus previous, OrderStatus next, int minute) {
        traces.add(new Trace(orderId + "-" + next, orderId, CLIENT_ID, RESTAURANT_ID, employeeId, previous, next,
                T0.plusMinutes(minute)));
    }

    /** Pedido completo: creado en startMinute y entregado en endMinute por el empleado. */
    private void deliveredOrder(Long orderId, Long employeeId, int startMinute, int endMinute) {
        trace(orderId, null, null, OrderStatus.PENDIENTE, startMinute);
        trace(orderId, employeeId, OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION, startMinute + 1);
        trace(orderId, employeeId, OrderStatus.EN_PREPARACION, OrderStatus.LISTO, endMinute - 1);
        trace(orderId, employeeId, OrderStatus.LISTO, OrderStatus.ENTREGADO, endMinute);
    }

    private void givenOwner() {
        when(restaurantClientPort.findRestaurantOwnerId(RESTAURANT_ID)).thenReturn(Optional.of(OWNER_ID));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(OWNER_ID);
    }

    @BeforeEach
    void setUp() {
        traces.clear();
    }

    // ---------- tiempo de inicio y fin de cada pedido ----------

    @Test
    void getRestaurantOrdersEfficiency_returnsStartEndAndDurationOfDeliveredOrders() {
        givenOwner();
        deliveredOrder(10L, EMPLOYEE_A, 0, 25);
        deliveredOrder(11L, EMPLOYEE_B, 5, 15);
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        List<OrderEfficiency> result = useCase.getRestaurantOrdersEfficiency(RESTAURANT_ID);

        assertEquals(2, result.size());
        OrderEfficiency first = result.get(0);
        assertEquals(10L, first.orderId());
        assertEquals(EMPLOYEE_A, first.employeeId());
        assertEquals(T0, first.startDate());
        assertEquals(T0.plusMinutes(25), first.endDate());
        assertEquals(1500, first.durationSeconds());
        assertEquals(25.0, first.durationMinutes());
        assertEquals(11L, result.get(1).orderId());
        assertEquals(10.0, result.get(1).durationMinutes());
    }

    @Test
    void getRestaurantOrdersEfficiency_excludesOrdersThatAreNotFinished() {
        givenOwner();
        deliveredOrder(10L, EMPLOYEE_A, 0, 20);
        // En proceso: sin fin.
        trace(12L, null, null, OrderStatus.PENDIENTE, 3);
        trace(12L, EMPLOYEE_A, OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION, 4);
        // Cancelado: no se entrego.
        trace(13L, null, null, OrderStatus.PENDIENTE, 6);
        trace(13L, null, OrderStatus.PENDIENTE, OrderStatus.CANCELADO, 7);
        // Entregado sin registro de inicio (datos incompletos): no se puede medir.
        trace(14L, EMPLOYEE_B, OrderStatus.LISTO, OrderStatus.ENTREGADO, 9);
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        List<OrderEfficiency> result = useCase.getRestaurantOrdersEfficiency(RESTAURANT_ID);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).orderId());
    }

    @Test
    void getRestaurantOrdersEfficiency_roundsMinutesToTwoDecimals() {
        givenOwner();
        trace(10L, null, null, OrderStatus.PENDIENTE, 0);
        traces.add(new Trace("x", 10L, CLIENT_ID, RESTAURANT_ID, EMPLOYEE_A, OrderStatus.LISTO, OrderStatus.ENTREGADO,
                T0.plusSeconds(100)));
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        OrderEfficiency result = useCase.getRestaurantOrdersEfficiency(RESTAURANT_ID).get(0);

        assertEquals(100, result.durationSeconds());
        assertEquals(1.67, result.durationMinutes());
    }

    @Test
    void getRestaurantOrdersEfficiency_usesLastAssignedEmployeeWhenDeliveryHasNone() {
        givenOwner();
        trace(10L, null, null, OrderStatus.PENDIENTE, 0);
        trace(10L, EMPLOYEE_B, OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION, 2);
        trace(10L, null, OrderStatus.LISTO, OrderStatus.ENTREGADO, 9);
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        assertEquals(EMPLOYEE_B, useCase.getRestaurantOrdersEfficiency(RESTAURANT_ID).get(0).employeeId());
    }

    @Test
    void getRestaurantOrdersEfficiency_withoutEmployeeInAnyTrace_returnsNullEmployee() {
        givenOwner();
        trace(10L, null, null, OrderStatus.PENDIENTE, 0);
        trace(10L, null, OrderStatus.LISTO, OrderStatus.ENTREGADO, 9);
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        assertNull(useCase.getRestaurantOrdersEfficiency(RESTAURANT_ID).get(0).employeeId());
    }

    @Test
    void getRestaurantOrdersEfficiency_withoutTraces_returnsEmptyList() {
        givenOwner();
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(List.of());

        assertTrue(useCase.getRestaurantOrdersEfficiency(RESTAURANT_ID).isEmpty());
    }

    // ---------- ranking de empleados ----------

    @Test
    void getEmployeeRanking_ordersEmployeesByAverageDurationFastestFirst() {
        givenOwner();
        deliveredOrder(10L, EMPLOYEE_A, 0, 30);   // A: 30 min
        deliveredOrder(11L, EMPLOYEE_A, 0, 20);   // A: 20 min -> media 25
        deliveredOrder(12L, EMPLOYEE_B, 0, 15);   // B: 15 min -> media 15
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        List<EmployeeRanking> ranking = useCase.getEmployeeRanking(RESTAURANT_ID);

        assertEquals(2, ranking.size());
        assertEquals(new EmployeeRanking(1, EMPLOYEE_B, 1, 15.0), ranking.get(0));
        assertEquals(new EmployeeRanking(2, EMPLOYEE_A, 2, 25.0), ranking.get(1));
    }

    @Test
    void getEmployeeRanking_onTieFavorsMoreCompletedOrdersThenLowerId() {
        givenOwner();
        deliveredOrder(10L, EMPLOYEE_B, 0, 10);
        deliveredOrder(11L, EMPLOYEE_A, 0, 10);
        deliveredOrder(12L, EMPLOYEE_A, 0, 10);
        deliveredOrder(13L, 7L, 0, 10);
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        List<EmployeeRanking> ranking = useCase.getEmployeeRanking(RESTAURANT_ID);

        assertEquals(EMPLOYEE_A, ranking.get(0).employeeId());
        assertEquals(7L, ranking.get(1).employeeId());
        assertEquals(EMPLOYEE_B, ranking.get(2).employeeId());
        assertEquals(3, ranking.get(2).position());
    }

    @Test
    void getEmployeeRanking_ignoresOrdersWithoutEmployee() {
        givenOwner();
        trace(10L, null, null, OrderStatus.PENDIENTE, 0);
        trace(10L, null, OrderStatus.LISTO, OrderStatus.ENTREGADO, 9);
        when(tracePersistencePort.findByRestaurantId(RESTAURANT_ID)).thenReturn(traces);

        assertTrue(useCase.getEmployeeRanking(RESTAURANT_ID).isEmpty());
    }

    // ---------- solo el propietario del restaurante ----------

    @Test
    void whenUserIsNotTheOwner_throwsException() {
        when(restaurantClientPort.findRestaurantOwnerId(RESTAURANT_ID)).thenReturn(Optional.of(OWNER_ID));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);

        assertThrows(NotRestaurantOwnerException.class, () -> useCase.getRestaurantOrdersEfficiency(RESTAURANT_ID));
        assertThrows(NotRestaurantOwnerException.class, () -> useCase.getEmployeeRanking(RESTAURANT_ID));
        verify(tracePersistencePort, never()).findByRestaurantId(any());
    }

    @Test
    void whenRestaurantDoesNotExist_throwsException() {
        when(restaurantClientPort.findRestaurantOwnerId(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> useCase.getEmployeeRanking(RESTAURANT_ID));
        verify(tracePersistencePort, never()).findByRestaurantId(any());
    }

    @Test
    void withNullRestaurantId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> useCase.getRestaurantOrdersEfficiency(null));
        verify(restaurantClientPort, never()).findRestaurantOwnerId(any());
    }
}
