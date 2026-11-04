package com.plazoleta.trazabilidad.application.handler.impl;

import com.plazoleta.trazabilidad.application.dto.response.EmployeeRankingResponseDto;
import com.plazoleta.trazabilidad.application.dto.response.OrderEfficiencyResponseDto;
import com.plazoleta.trazabilidad.application.handler.IOrderEfficiencyHandler;
import com.plazoleta.trazabilidad.domain.api.IOrderEfficiencyServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderEfficiencyHandler implements IOrderEfficiencyHandler {

    private final IOrderEfficiencyServicePort orderEfficiencyServicePort;

    @Override
    public List<OrderEfficiencyResponseDto> getRestaurantOrdersEfficiency(Long restaurantId) {
        return orderEfficiencyServicePort.getRestaurantOrdersEfficiency(restaurantId).stream()
                .map(o -> new OrderEfficiencyResponseDto(o.orderId(), o.employeeId(), o.startDate(), o.endDate(),
                        o.durationSeconds(), o.durationMinutes()))
                .toList();
    }

    @Override
    public List<EmployeeRankingResponseDto> getEmployeeRanking(Long restaurantId) {
        return orderEfficiencyServicePort.getEmployeeRanking(restaurantId).stream()
                .map(r -> new EmployeeRankingResponseDto(r.position(), r.employeeId(), r.completedOrders(),
                        r.averageDurationMinutes()))
                .toList();
    }
}
