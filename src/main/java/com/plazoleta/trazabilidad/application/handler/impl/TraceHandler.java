package com.plazoleta.trazabilidad.application.handler.impl;

import com.plazoleta.trazabilidad.application.dto.request.TraceRequestDto;
import com.plazoleta.trazabilidad.application.dto.response.TraceResponseDto;
import com.plazoleta.trazabilidad.application.handler.ITraceHandler;
import com.plazoleta.trazabilidad.application.mapper.TraceDtoMapper;
import com.plazoleta.trazabilidad.domain.api.ITraceServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TraceHandler implements ITraceHandler {

    private final ITraceServicePort traceServicePort;
    private final TraceDtoMapper traceDtoMapper;

    @Override
    public TraceResponseDto saveTrace(TraceRequestDto traceRequestDto) {
        return traceDtoMapper.toResponse(traceServicePort.saveTrace(traceDtoMapper.toTrace(traceRequestDto)));
    }

    @Override
    public List<TraceResponseDto> getOrderTraces(Long orderId) {
        return traceServicePort.getOrderTraces(orderId).stream().map(traceDtoMapper::toResponse).toList();
    }
}
