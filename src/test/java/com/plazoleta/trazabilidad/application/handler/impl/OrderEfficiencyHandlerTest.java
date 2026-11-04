package com.plazoleta.trazabilidad.application.handler.impl;

import com.plazoleta.trazabilidad.application.dto.response.EmployeeRankingResponseDto;
import com.plazoleta.trazabilidad.application.dto.response.OrderEfficiencyResponseDto;
import com.plazoleta.trazabilidad.domain.api.IOrderEfficiencyServicePort;
import com.plazoleta.trazabilidad.domain.model.EmployeeRanking;
import com.plazoleta.trazabilidad.domain.model.OrderEfficiency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEfficiencyHandlerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 4, 10, 0);

    @Mock
    private IOrderEfficiencyServicePort orderEfficiencyServicePort;

    @InjectMocks
    private OrderEfficiencyHandler handler;

    @Test
    void getRestaurantOrdersEfficiency_mapsEveryOrder() {
        when(orderEfficiencyServicePort.getRestaurantOrdersEfficiency(1L)).thenReturn(List.of(
                new OrderEfficiency(10L, 8L, START, START.plusMinutes(25), 1500, 25.0)));

        OrderEfficiencyResponseDto dto = handler.getRestaurantOrdersEfficiency(1L).get(0);

        assertEquals(10L, dto.getOrderId());
        assertEquals(8L, dto.getEmployeeId());
        assertEquals(START, dto.getStartDate());
        assertEquals(START.plusMinutes(25), dto.getEndDate());
        assertEquals(1500, dto.getDurationSeconds());
        assertEquals(25.0, dto.getDurationMinutes());
    }

    @Test
    void getEmployeeRanking_mapsEveryPosition() {
        when(orderEfficiencyServicePort.getEmployeeRanking(1L)).thenReturn(List.of(
                new EmployeeRanking(1, 9L, 3, 12.5), new EmployeeRanking(2, 8L, 1, 20.0)));

        List<EmployeeRankingResponseDto> result = handler.getEmployeeRanking(1L);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getPosition());
        assertEquals(9L, result.get(0).getEmployeeId());
        assertEquals(3, result.get(0).getCompletedOrders());
        assertEquals(12.5, result.get(0).getAverageDurationMinutes());
        assertEquals(8L, result.get(1).getEmployeeId());
    }
}
