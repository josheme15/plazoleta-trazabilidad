package com.plazoleta.trazabilidad.domain.model;

import java.time.LocalDateTime;

/**
 * Registro de un cambio de estado de un pedido.
 */
public class Trace {

    private String id;
    private Long orderId;
    private Long clientId;
    private Long restaurantId;
    private Long employeeId;
    // null cuando el pedido se crea (no tenia estado anterior).
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private LocalDateTime date;

    public Trace(String id, Long orderId, Long clientId, Long restaurantId, Long employeeId,
                 OrderStatus previousStatus, OrderStatus newStatus, LocalDateTime date) {
        this.id = id;
        this.orderId = orderId;
        this.clientId = clientId;
        this.restaurantId = restaurantId;
        this.employeeId = employeeId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public OrderStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(OrderStatus previousStatus) { this.previousStatus = previousStatus; }

    public OrderStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderStatus newStatus) { this.newStatus = newStatus; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}
