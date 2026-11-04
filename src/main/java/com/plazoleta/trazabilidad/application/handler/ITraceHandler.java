package com.plazoleta.trazabilidad.application.handler;

import com.plazoleta.trazabilidad.application.dto.request.TraceRequestDto;
import com.plazoleta.trazabilidad.application.dto.response.TraceResponseDto;

import java.util.List;

public interface ITraceHandler {

    TraceResponseDto saveTrace(TraceRequestDto traceRequestDto);

    List<TraceResponseDto> getOrderTraces(Long orderId);
}
