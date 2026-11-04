package com.plazoleta.trazabilidad.infrastructure.output.client.adapter;

import com.plazoleta.trazabilidad.infrastructure.output.client.exception.PlazoletaServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestaurantRestClientAdapterTest {

    private static final String BASE_URL = "http://plazoleta";
    private static final String TOKEN = "jwt-del-propietario";

    private MockRestServiceServer server;
    private RestaurantRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new RestaurantRestClientAdapter(builder.build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("propietario", TOKEN, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findRestaurantOwnerId_returnsOwnerAndForwardsToken() {
        server.expect(requestTo(BASE_URL + "/restaurants/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess("{\"id\":1,\"name\":\"El Sabor\",\"nit\":\"900123\",\"ownerId\":2}",
                        MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(2L), adapter.findRestaurantOwnerId(1L));
        server.verify();
    }

    @Test
    void findRestaurantOwnerId_withoutAuthentication_sendsNoToken() {
        SecurityContextHolder.clearContext();
        server.expect(requestTo(BASE_URL + "/restaurants/1"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("{\"id\":1,\"ownerId\":2}", MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(2L), adapter.findRestaurantOwnerId(1L));
    }

    @Test
    void findRestaurantOwnerId_whenRestaurantDoesNotExist_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/restaurants/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertEquals(Optional.empty(), adapter.findRestaurantOwnerId(99L));
    }

    @Test
    void findRestaurantOwnerId_whenPlazoletaFails_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/restaurants/1")).andRespond(withServerError());

        assertThrows(PlazoletaServiceUnavailableException.class, () -> adapter.findRestaurantOwnerId(1L));
    }

    @Test
    void findRestaurantOwnerId_whenPlazoletaRejectsToken_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/restaurants/1")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(PlazoletaServiceUnavailableException.class, () -> adapter.findRestaurantOwnerId(1L));
    }

    @Test
    void findRestaurantOwnerId_withEmptyBody_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/restaurants/1")).andRespond(withSuccess());

        assertEquals(Optional.empty(), adapter.findRestaurantOwnerId(1L));
    }
}
