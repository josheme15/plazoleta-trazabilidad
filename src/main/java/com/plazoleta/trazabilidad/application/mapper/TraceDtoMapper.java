package com.plazoleta.trazabilidad.application.mapper;

import com.plazoleta.trazabilidad.application.dto.request.TraceRequestDto;
import com.plazoleta.trazabilidad.application.dto.response.TraceResponseDto;
import com.plazoleta.trazabilidad.domain.model.Trace;
import org.springframework.stereotype.Component;

@Component
public class TraceDtoMapper {

    public Trace toTrace(TraceRequestDto dto) {
        return new Trace(null, dto.getOrderId(), dto.getClientId(), dto.getRestaurantId(), dto.getEmployeeId(),
                dto.getPreviousStatus(), dto.getNewStatus(), null);
    }

    public TraceResponseDto toResponse(Trace trace) {
        return new TraceResponseDto(trace.getId(), trace.getOrderId(), trace.getClientId(), trace.getRestaurantId(),
                trace.getEmployeeId(), trace.getPreviousStatus(), trace.getNewStatus(), trace.getDate());
    }
}
