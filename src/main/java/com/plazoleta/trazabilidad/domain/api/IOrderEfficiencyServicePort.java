package com.plazoleta.trazabilidad.domain.api;

import com.plazoleta.trazabilidad.domain.model.EmployeeRanking;
import com.plazoleta.trazabilidad.domain.model.OrderEfficiency;

import java.util.List;

/**
 * HU-18: eficiencia de los pedidos de un restaurante, solo para su propietario.
 */
public interface IOrderEfficiencyServicePort {

    /**
     * Inicio, fin y duracion de cada pedido entregado del restaurante, del mas antiguo al mas reciente.
     */
    List<OrderEfficiency> getRestaurantOrdersEfficiency(Long restaurantId);

    /**
     * Ranking de empleados del restaurante por tiempo medio entre que un pedido inicia y se termina (del mas rapido
     * al mas lento).
     */
    List<EmployeeRanking> getEmployeeRanking(Long restaurantId);
}
