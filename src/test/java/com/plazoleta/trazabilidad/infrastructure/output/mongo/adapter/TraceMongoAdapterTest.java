package com.plazoleta.trazabilidad.infrastructure.output.mongo.adapter;

import com.plazoleta.trazabilidad.domain.model.OrderStatus;
import com.plazoleta.trazabilidad.domain.model.Trace;
import com.plazoleta.trazabilidad.infrastructure.output.mongo.document.TraceDocument;
import com.plazoleta.trazabilidad.infrastructure.output.mongo.repository.ITraceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceMongoAdapterTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 11, 4, 10, 0);

    @Mock
    private ITraceRepository traceRepository;

    @InjectMocks
    private TraceMongoAdapter adapter;

    @Test
    void saveTrace_mapsDomainToDocumentAndBack() {
        when(traceRepository.save(any(TraceDocument.class))).thenAnswer(inv -> {
            TraceDocument document = inv.getArgument(0);
            document.setId("generado");
            return document;
        });

        Trace result = adapter.saveTrace(
                new Trace(null, 10L, 30L, 1L, 8L, OrderStatus.LISTO, OrderStatus.ENTREGADO, DATE));

        ArgumentCaptor<TraceDocument> captor = ArgumentCaptor.forClass(TraceDocument.class);
        verify(traceRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getOrderId());
        assertEquals(OrderStatus.ENTREGADO, captor.getValue().getNewStatus());
        assertEquals("generado", result.getId());
        assertEquals(30L, result.getClientId());
        assertEquals(1L, result.getRestaurantId());
        assertEquals(8L, result.getEmployeeId());
        assertEquals(OrderStatus.LISTO, result.getPreviousStatus());
        assertEquals(DATE, result.getDate());
    }

    @Test
    void findByOrderId_sortsByDateAndId() {
        when(traceRepository.findByOrderId(eq(10L), any(Sort.class))).thenReturn(List.of(
                new TraceDocument("1", 10L, 30L, 1L, null, null, OrderStatus.PENDIENTE, DATE)));

        List<Trace> result = adapter.findByOrderId(10L);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(traceRepository).findByOrderId(eq(10L), sortCaptor.capture());
        assertEquals(Sort.by(Sort.Order.asc("date"), Sort.Order.asc("id")), sortCaptor.getValue());
        assertEquals(1, result.size());
        assertEquals(OrderStatus.PENDIENTE, result.get(0).getNewStatus());
    }

    @Test
    void findByRestaurantId_sortsByDateAndId() {
        when(traceRepository.findByRestaurantId(eq(1L), any(Sort.class))).thenReturn(List.of(
                new TraceDocument("1", 10L, 30L, 1L, 8L, OrderStatus.LISTO, OrderStatus.ENTREGADO, DATE)));

        List<Trace> result = adapter.findByRestaurantId(1L);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(traceRepository).findByRestaurantId(eq(1L), sortCaptor.capture());
        assertEquals(Sort.by(Sort.Order.asc("date"), Sort.Order.asc("id")), sortCaptor.getValue());
        assertEquals(OrderStatus.ENTREGADO, result.get(0).getNewStatus());
    }
}
