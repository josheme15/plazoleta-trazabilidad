package com.plazoleta.trazabilidad.infrastructure.security;

import com.plazoleta.trazabilidad.application.dto.response.TraceResponseDto;
import com.plazoleta.trazabilidad.application.handler.ITraceHandler;
import com.plazoleta.trazabilidad.domain.exception.OrderFromAnotherClientException;
import com.plazoleta.trazabilidad.domain.exception.RequiredFieldException;
import com.plazoleta.trazabilidad.domain.exception.TraceNotAllowedException;
import com.plazoleta.trazabilidad.domain.exception.TraceNotFoundException;
import com.plazoleta.trazabilidad.domain.model.OrderStatus;
import com.plazoleta.trazabilidad.infrastructure.exceptionhandler.ControllerAdvisor;
import com.plazoleta.trazabilidad.infrastructure.input.rest.TraceRestController;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-17: reglas de rol de los endpoints de trazabilidad y respuestas de error en JSON.
 */
@WebMvcTest(controllers = TraceRestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, ControllerAdvisor.class})
@TestPropertySource(properties = "security.jwt.secret=" + EndpointSecurityTest.SECRET)
class EndpointSecurityTest {

    static final String SECRET = "cGxhem9sZXRhLWRlLWNvbWlkYXMtc2VjcmV0by1zdXBlci1zZWd1cm8tMjU2LWJpdHM=";
    private static final String BODY = """
            {"orderId":10,"clientId":30,"restaurantId":1,"employeeId":8,
             "previousStatus":"PENDIENTE","newStatus":"EN_PREPARACION"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ITraceHandler traceHandler;

    /** Token firmado con la misma clave que usa plazoleta-usuarios. */
    private String bearer(String role) {
        Date now = new Date();
        String token = Jwts.builder()
                .subject("usuario@plazoleta.com")
                .claim("id", 30L)
                .claim("role", role)
                .claim("restaurantId", 1L)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        return "Bearer " + token;
    }

    private TraceResponseDto response(OrderStatus previous, OrderStatus next) {
        return new TraceResponseDto("abc", 10L, 30L, 1L, 8L, previous, next, LocalDateTime.of(2026, 11, 4, 10, 0));
    }

    // ---------- POST /traces: CLIENT o EMPLOYEE ----------

    @ParameterizedTest
    @ValueSource(strings = {"CLIENT", "EMPLOYEE"})
    void saveTrace_withClientOrEmployee_returns201(String role) throws Exception {
        when(traceHandler.saveTrace(any())).thenReturn(response(OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION));

        mockMvc.perform(post("/traces").header(HttpHeaders.AUTHORIZATION, bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newStatus").value("EN_PREPARACION"))
                .andExpect(jsonPath("$.date").value("2026-11-04T10:00:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OWNER"})
    void saveTrace_withOtherRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/traces").header(HttpHeaders.AUTHORIZATION, bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveTrace_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/traces").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void saveTrace_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(post("/traces").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saveTrace_withoutRequiredFields_returns400WithDetails() throws Exception {
        mockMvc.perform(post("/traces").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(4));
    }

    @Test
    void saveTrace_withInvalidStatus_returns400() throws Exception {
        mockMvc.perform(post("/traces").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":10,\"clientId\":30,\"restaurantId\":1,\"newStatus\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveTrace_whenOrderDoesNotBelongToTheUser_returns403() throws Exception {
        when(traceHandler.saveTrace(any())).thenThrow(new TraceNotAllowedException());

        mockMvc.perform(post("/traces").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("No puede registrar cambios de estado de un pedido que no le corresponde"));
    }

    @Test
    void saveTrace_whenDomainRejects_returns400() throws Exception {
        when(traceHandler.saveTrace(any())).thenThrow(new RequiredFieldException("idPedido"));

        mockMvc.perform(post("/traces").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /traces/orders/{orderId}: solo CLIENT ----------

    @Test
    void getOrderTraces_asClient_returnsHistory() throws Exception {
        when(traceHandler.getOrderTraces(10L)).thenReturn(List.of(
                response(null, OrderStatus.PENDIENTE), response(OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION)));

        mockMvc.perform(get("/traces/orders/10").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].newStatus").value("PENDIENTE"))
                .andExpect(jsonPath("$[1].previousStatus").value("PENDIENTE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OWNER", "EMPLOYEE"})
    void getOrderTraces_withOtherRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/traces/orders/10").header(HttpHeaders.AUTHORIZATION, bearer(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrderTraces_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/traces/orders/10")).andExpect(status().isUnauthorized());
    }

    @Test
    void getOrderTraces_ofAnotherClient_returns403() throws Exception {
        when(traceHandler.getOrderTraces(10L)).thenThrow(new OrderFromAnotherClientException());

        mockMvc.perform(get("/traces/orders/10").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Solo puede consultar la trazabilidad de sus propios pedidos"));
    }

    @Test
    void getOrderTraces_withoutTraces_returns404() throws Exception {
        when(traceHandler.getOrderTraces(10L)).thenThrow(new TraceNotFoundException());

        mockMvc.perform(get("/traces/orders/10").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderTraces_withNonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/traces/orders/abc").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isBadRequest());
    }
}
