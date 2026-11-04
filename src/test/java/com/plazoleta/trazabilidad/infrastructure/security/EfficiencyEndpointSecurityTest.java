package com.plazoleta.trazabilidad.infrastructure.security;

import com.plazoleta.trazabilidad.application.dto.response.EmployeeRankingResponseDto;
import com.plazoleta.trazabilidad.application.dto.response.OrderEfficiencyResponseDto;
import com.plazoleta.trazabilidad.application.handler.IOrderEfficiencyHandler;
import com.plazoleta.trazabilidad.domain.exception.NotRestaurantOwnerException;
import com.plazoleta.trazabilidad.domain.exception.RestaurantNotFoundException;
import com.plazoleta.trazabilidad.infrastructure.exceptionhandler.ControllerAdvisor;
import com.plazoleta.trazabilidad.infrastructure.input.rest.OrderEfficiencyRestController;
import com.plazoleta.trazabilidad.infrastructure.output.client.exception.PlazoletaServiceUnavailableException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-18: los endpoints de eficiencia son solo para el PROPIETARIO y responden los errores en JSON.
 */
@WebMvcTest(controllers = OrderEfficiencyRestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, ControllerAdvisor.class})
@TestPropertySource(properties = "security.jwt.secret=" + EfficiencyEndpointSecurityTest.SECRET)
class EfficiencyEndpointSecurityTest {

    static final String SECRET = "cGxhem9sZXRhLWRlLWNvbWlkYXMtc2VjcmV0by1zdXBlci1zZWd1cm8tMjU2LWJpdHM=";
    private static final String ORDERS = "/traces/restaurants/1/orders";
    private static final String RANKING = "/traces/restaurants/1/employees/ranking";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOrderEfficiencyHandler orderEfficiencyHandler;

    private String bearer(String role) {
        Date now = new Date();
        String token = Jwts.builder()
                .subject("propietario@plazoleta.com")
                .claim("id", 2L)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        return "Bearer " + token;
    }

    @Test
    void getOrdersEfficiency_asOwner_returnsStartEndAndDuration() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 11, 4, 10, 0);
        when(orderEfficiencyHandler.getRestaurantOrdersEfficiency(1L)).thenReturn(List.of(
                new OrderEfficiencyResponseDto(10L, 8L, start, start.plusMinutes(25), 1500, 25.0)));

        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(10))
                .andExpect(jsonPath("$[0].startDate").value("2026-11-04T10:00:00"))
                .andExpect(jsonPath("$[0].endDate").value("2026-11-04T10:25:00"))
                .andExpect(jsonPath("$[0].durationMinutes").value(25.0));
    }

    @Test
    void getEmployeeRanking_asOwner_returnsRanking() throws Exception {
        when(orderEfficiencyHandler.getEmployeeRanking(1L)).thenReturn(List.of(
                new EmployeeRankingResponseDto(1, 9L, 3, 12.5), new EmployeeRankingResponseDto(2, 8L, 1, 20.0)));

        mockMvc.perform(get(RANKING).header(HttpHeaders.AUTHORIZATION, bearer("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(9))
                .andExpect(jsonPath("$[1].averageDurationMinutes").value(20.0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EMPLOYEE", "CLIENT"})
    void withOtherRole_returns403(String role) throws Exception {
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(role))).andExpect(status().isForbidden());
        mockMvc.perform(get(RANKING).header(HttpHeaders.AUTHORIZATION, bearer(role))).andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get(ORDERS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RANKING)).andExpect(status().isUnauthorized());
    }

    @Test
    void whenNotOwnerOfTheRestaurant_returns403() throws Exception {
        when(orderEfficiencyHandler.getEmployeeRanking(1L)).thenThrow(new NotRestaurantOwnerException());

        mockMvc.perform(get(RANKING).header(HttpHeaders.AUTHORIZATION, bearer("OWNER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Solo el propietario del restaurante puede consultar la eficiencia de sus pedidos"));
    }

    @Test
    void whenRestaurantDoesNotExist_returns404() throws Exception {
        when(orderEfficiencyHandler.getRestaurantOrdersEfficiency(1L)).thenThrow(new RestaurantNotFoundException());

        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer("OWNER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenPlazoletaIsDown_returns503() throws Exception {
        when(orderEfficiencyHandler.getRestaurantOrdersEfficiency(1L))
                .thenThrow(new PlazoletaServiceUnavailableException(new IllegalStateException("caido")));

        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer("OWNER")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void withNonNumericRestaurantId_returns400() throws Exception {
        mockMvc.perform(get("/traces/restaurants/abc/orders").header(HttpHeaders.AUTHORIZATION, bearer("OWNER")))
                .andExpect(status().isBadRequest());
    }
}
