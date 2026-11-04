package com.plazoleta.trazabilidad.infrastructure.output.client.adapter;

import com.plazoleta.trazabilidad.domain.spi.IRestaurantClientPort;
import com.plazoleta.trazabilidad.infrastructure.output.client.dto.RestaurantClientResponseDto;
import com.plazoleta.trazabilidad.infrastructure.output.client.exception.PlazoletaServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Consulta el restaurante en plazoleta reenviando el JWT de la peticion actual.
 */
@RequiredArgsConstructor
public class RestaurantRestClientAdapter implements IRestaurantClientPort {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient plazoletaRestClient;

    @Override
    public Optional<Long> findRestaurantOwnerId(Long restaurantId) {
        try {
            RestaurantClientResponseDto restaurant = plazoletaRestClient.get()
                    .uri("/restaurants/{id}", restaurantId)
                    .headers(RestaurantRestClientAdapter::addCurrentToken)
                    .retrieve()
                    .body(RestaurantClientResponseDto.class);
            return Optional.ofNullable(restaurant).map(RestaurantClientResponseDto::ownerId);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new PlazoletaServiceUnavailableException(e);
        }
    }

    private static void addCurrentToken(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
        }
    }
}
