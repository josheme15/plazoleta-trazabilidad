package com.plazoleta.trazabilidad.infrastructure.output.mongo.adapter;

import com.plazoleta.trazabilidad.domain.model.Trace;
import com.plazoleta.trazabilidad.domain.spi.ITracePersistencePort;
import com.plazoleta.trazabilidad.infrastructure.output.mongo.document.TraceDocument;
import com.plazoleta.trazabilidad.infrastructure.output.mongo.repository.ITraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class TraceMongoAdapter implements ITracePersistencePort {

    // Del mas antiguo al mas reciente; el id (ObjectId, creciente) desempata registros con la misma fecha.
    private static final Sort CHRONOLOGICAL = Sort.by(Sort.Order.asc("date"), Sort.Order.asc("id"));

    private final ITraceRepository traceRepository;

    @Override
    public Trace saveTrace(Trace trace) {
        TraceDocument document = new TraceDocument(trace.getId(), trace.getOrderId(), trace.getClientId(),
                trace.getRestaurantId(), trace.getEmployeeId(), trace.getPreviousStatus(), trace.getNewStatus(),
                trace.getDate());
        return toTrace(traceRepository.save(document));
    }

    @Override
    public List<Trace> findByOrderId(Long orderId) {
        return traceRepository.findByOrderId(orderId, CHRONOLOGICAL).stream().map(this::toTrace).toList();
    }

    @Override
    public List<Trace> findByRestaurantId(Long restaurantId) {
        return traceRepository.findByRestaurantId(restaurantId, CHRONOLOGICAL).stream().map(this::toTrace).toList();
    }

    private Trace toTrace(TraceDocument document) {
        return new Trace(document.getId(), document.getOrderId(), document.getClientId(), document.getRestaurantId(),
                document.getEmployeeId(), document.getPreviousStatus(), document.getNewStatus(), document.getDate());
    }
}
