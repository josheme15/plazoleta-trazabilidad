package com.plazoleta.trazabilidad.application.handler.impl;

import com.plazoleta.trazabilidad.application.dto.request.TraceRequestDto;
import com.plazoleta.trazabilidad.application.dto.response.TraceResponseDto;
import com.plazoleta.trazabilidad.application.mapper.TraceDtoMapper;
import com.plazoleta.trazabilidad.domain.api.ITraceServicePort;
import com.plazoleta.trazabilidad.domain.model.OrderStatus;
import com.plazoleta.trazabilidad.domain.model.Trace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceHandlerTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 11, 4, 10, 0);

    @Mock
    private ITraceServicePort traceServicePort;

    private TraceHandler traceHandler;

    @BeforeEach
    void setUp() {
        traceHandler = new TraceHandler(traceServicePort, new TraceDtoMapper());
    }

    @Test
    void saveTrace_mapsRequestToDomainAndResponse() {
        when(traceServicePort.saveTrace(any(Trace.class))).thenAnswer(inv -> {
            Trace trace = inv.getArgument(0);
            trace.setId("abc");
            trace.setDate(DATE);
            return trace;
        });

        TraceResponseDto response = traceHandler.saveTrace(
                new TraceRequestDto(10L, 30L, 1L, 8L, OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION));

        ArgumentCaptor<Trace> captor = ArgumentCaptor.forClass(Trace.class);
        verify(traceServicePort).saveTrace(captor.capture());
        assertEquals(30L, captor.getValue().getClientId());
        assertEquals("abc", response.getId());
        assertEquals(10L, response.getOrderId());
        assertEquals(1L, response.getRestaurantId());
        assertEquals(8L, response.getEmployeeId());
        assertEquals(OrderStatus.PENDIENTE, response.getPreviousStatus());
        assertEquals(OrderStatus.EN_PREPARACION, response.getNewStatus());
        assertEquals(DATE, response.getDate());
    }

    @Test
    void getOrderTraces_mapsEveryTrace() {
        when(traceServicePort.getOrderTraces(10L)).thenReturn(List.of(
                new Trace("1", 10L, 30L, 1L, null, null, OrderStatus.PENDIENTE, DATE),
                new Trace("2", 10L, 30L, 1L, null, OrderStatus.PENDIENTE, OrderStatus.CANCELADO, DATE)));

        List<TraceResponseDto> result = traceHandler.getOrderTraces(10L);

        assertEquals(2, result.size());
        assertEquals(OrderStatus.PENDIENTE, result.get(0).getNewStatus());
        assertEquals(OrderStatus.CANCELADO, result.get(1).getNewStatus());
    }
}
