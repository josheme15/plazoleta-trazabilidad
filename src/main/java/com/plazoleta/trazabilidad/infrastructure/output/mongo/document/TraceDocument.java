package com.plazoleta.trazabilidad.infrastructure.output.mongo.document;

import com.plazoleta.trazabilidad.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "traces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TraceDocument {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    @Indexed
    private Long clientId;

    @Indexed
    private Long restaurantId;
    private Long employeeId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private LocalDateTime date;
}
