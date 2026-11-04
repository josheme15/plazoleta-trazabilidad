package com.plazoleta.trazabilidad.application.handler;

import com.plazoleta.trazabilidad.application.dto.response.EmployeeRankingResponseDto;
import com.plazoleta.trazabilidad.application.dto.response.OrderEfficiencyResponseDto;

import java.util.List;

public interface IOrderEfficiencyHandler {

    List<OrderEfficiencyResponseDto> getRestaurantOrdersEfficiency(Long restaurantId);

    List<EmployeeRankingResponseDto> getEmployeeRanking(Long restaurantId);
}
