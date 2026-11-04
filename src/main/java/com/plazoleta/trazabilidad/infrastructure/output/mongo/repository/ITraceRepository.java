package com.plazoleta.trazabilidad.infrastructure.output.mongo.repository;

import com.plazoleta.trazabilidad.infrastructure.output.mongo.document.TraceDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ITraceRepository extends MongoRepository<TraceDocument, String> {

    List<TraceDocument> findByOrderId(Long orderId, Sort sort);

    List<TraceDocument> findByRestaurantId(Long restaurantId, Sort sort);
}
