package com.plazoleta.trazabilidad.domain.usecase;

import com.plazoleta.trazabilidad.domain.exception.OrderFromAnotherClientException;
import com.plazoleta.trazabilidad.domain.exception.RequiredFieldException;
import com.plazoleta.trazabilidad.domain.exception.TraceNotAllowedException;
import com.plazoleta.trazabilidad.domain.exception.TraceNotFoundException;
import com.plazoleta.trazabilidad.domain.model.OrderStatus;
import com.plazoleta.trazabilidad.domain.model.Trace;
import com.plazoleta.trazabilidad.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.trazabilidad.domain.spi.ITracePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceUseCaseTest {

    private static final Long ORDER_ID = 10L;
    private static final Long CLIENT_ID = 30L;
    private static final Long RESTAURANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 8L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 15, 30);

    @Mock
    private ITracePersistencePort tracePersistencePort;

    @Mock
    private IAuthenticatedUserPort authenticatedUserPort;

    private TraceUseCase traceUseCase;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        traceUseCase = new TraceUseCase(tracePersistencePort, authenticatedUserPort, fixedClock);
    }

    private Trace trace(OrderStatus previous, OrderStatus next) {
        return new Trace("id-viejo", ORDER_ID, CLIENT_ID, RESTAURANT_ID, EMPLOYEE_ID, previous, next,
                LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    // ---------- registrar un cambio de estado ----------

    @Test
    void saveTrace_byClientOwnerOfTheOrder_setsDateAndSaves() {
        when(authenticatedUserPort.getAuthenticatedRole()).thenReturn("CLIENT");
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(tracePersistencePort.saveTrace(any(Trace.class))).thenAnswer(inv -> inv.getArgument(0));

        Trace result = traceUseCase.saveTrace(trace(null, OrderStatus.PENDIENTE));

        ArgumentCaptor<Trace> captor = ArgumentCaptor.forClass(Trace.class);
        verify(tracePersistencePort).saveTrace(captor.capture());
        Trace saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals(NOW, saved.getDate());
        assertNull(saved.getPreviousStatus());
        assertEquals(OrderStatus.PENDIENTE, saved.getNewStatus());
        assertSame(saved, result);
    }

    @Test
    void saveTrace_byEmployeeOfTheOrderRestaurant_saves() {
        when(authenticatedUserPort.getAuthenticatedRole()).thenReturn("EMPLOYEE");
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(tracePersistencePort.saveTrace(any(Trace.class))).thenAnswer(inv -> inv.getArgument(0));

        Trace result = traceUseCase.saveTrace(trace(OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION));

        assertEquals(OrderStatus.PENDIENTE, result.getPreviousStatus());
        assertEquals(OrderStatus.EN_PREPARACION, result.getNewStatus());
        assertEquals(EMPLOYEE_ID, result.getEmployeeId());
        assertEquals(NOW, result.getDate());
    }

    @Test
    void saveTrace_byClientOfAnotherOrder_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRole()).thenReturn("CLIENT");
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);
        Trace trace = trace(null, OrderStatus.PENDIENTE);

        assertThrows(TraceNotAllowedException.class, () -> traceUseCase.saveTrace(trace));
        verify(tracePersistencePort, never()).saveTrace(any());
    }

    @Test
    void saveTrace_byEmployeeOfAnotherRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRole()).thenReturn("EMPLOYEE");
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(2L);
        Trace trace = trace(OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION);

        assertThrows(TraceNotAllowedException.class, () -> traceUseCase.saveTrace(trace));
        verify(tracePersistencePort, never()).saveTrace(any());
    }

    @Test
    void saveTrace_byEmployeeWithoutRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRole()).thenReturn("EMPLOYEE");
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);
        Trace trace = trace(OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION);

        assertThrows(TraceNotAllowedException.class, () -> traceUseCase.saveTrace(trace));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"ADMIN", "OWNER"})
    void saveTrace_withOtherRole_throwsException(String role) {
        when(authenticatedUserPort.getAuthenticatedRole()).thenReturn(role);
        Trace trace = trace(null, OrderStatus.PENDIENTE);

        assertThrows(TraceNotAllowedException.class, () -> traceUseCase.saveTrace(trace));
        verify(tracePersistencePort, never()).saveTrace(any());
    }

    @Test
    void saveTrace_withoutOrderId_throwsException() {
        Trace trace = trace(null, OrderStatus.PENDIENTE);
        trace.setOrderId(null);

        assertThrows(RequiredFieldException.class, () -> traceUseCase.saveTrace(trace));
    }

    @Test
    void saveTrace_withoutClientId_throwsException() {
        Trace trace = trace(null, OrderStatus.PENDIENTE);
        trace.setClientId(null);

        assertThrows(RequiredFieldException.class, () -> traceUseCase.saveTrace(trace));
    }

    @Test
    void saveTrace_withoutRestaurantId_throwsException() {
        Trace trace = trace(null, OrderStatus.PENDIENTE);
        trace.setRestaurantId(null);

        assertThrows(RequiredFieldException.class, () -> traceUseCase.saveTrace(trace));
    }

    @Test
    void saveTrace_withoutNewStatus_throwsException() {
        Trace trace = trace(OrderStatus.PENDIENTE, null);

        assertThrows(RequiredFieldException.class, () -> traceUseCase.saveTrace(trace));
        verify(tracePersistencePort, never()).saveTrace(any());
    }

    // ---------- consultar el historial ----------

    @Test
    void getOrderTraces_ownOrder_returnsTracesInOrder() {
        List<Trace> traces = List.of(trace(null, OrderStatus.PENDIENTE),
                trace(OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION));
        when(tracePersistencePort.findByOrderId(ORDER_ID)).thenReturn(traces);
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);

        assertSame(traces, traceUseCase.getOrderTraces(ORDER_ID));
    }

    @Test
    void getOrderTraces_orderOfAnotherClient_throwsException() {
        when(tracePersistencePort.findByOrderId(ORDER_ID)).thenReturn(List.of(trace(null, OrderStatus.PENDIENTE)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);

        assertThrows(OrderFromAnotherClientException.class, () -> traceUseCase.getOrderTraces(ORDER_ID));
    }

    @Test
    void getOrderTraces_withoutTraces_throwsException() {
        when(tracePersistencePort.findByOrderId(ORDER_ID)).thenReturn(List.of());

        assertThrows(TraceNotFoundException.class, () -> traceUseCase.getOrderTraces(ORDER_ID));
        verify(authenticatedUserPort, never()).getAuthenticatedUserId();
    }

    @Test
    void getOrderTraces_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> traceUseCase.getOrderTraces(null));
        verify(tracePersistencePort, never()).findByOrderId(any());
    }
}
