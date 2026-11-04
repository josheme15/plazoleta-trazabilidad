package com.plazoleta.trazabilidad.infrastructure.configuration;

import com.plazoleta.trazabilidad.domain.api.IOrderEfficiencyServicePort;
import com.plazoleta.trazabilidad.domain.api.ITraceServicePort;
import com.plazoleta.trazabilidad.domain.spi.IRestaurantClientPort;
import com.plazoleta.trazabilidad.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.trazabilidad.domain.spi.ITracePersistencePort;
import com.plazoleta.trazabilidad.domain.usecase.OrderEfficiencyUseCase;
import com.plazoleta.trazabilidad.domain.usecase.TraceUseCase;
import com.plazoleta.trazabilidad.infrastructure.output.client.adapter.RestaurantRestClientAdapter;
import com.plazoleta.trazabilidad.infrastructure.output.mongo.adapter.TraceMongoAdapter;
import com.plazoleta.trazabilidad.infrastructure.output.mongo.repository.ITraceRepository;
import com.plazoleta.trazabilidad.infrastructure.security.AuthenticatedUserAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
public class BeanConfiguration {

    @Value("${clients.plazoleta.url}")
    private String plazoletaUrl;

    @Bean
    public RestClient plazoletaRestClient(RestClient.Builder builder) {
        return builder.clone().baseUrl(plazoletaUrl).build();
    }

    @Bean
    public IRestaurantClientPort restaurantClientPort(RestClient plazoletaRestClient) {
        return new RestaurantRestClientAdapter(plazoletaRestClient);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ITracePersistencePort tracePersistencePort(ITraceRepository traceRepository) {
        return new TraceMongoAdapter(traceRepository);
    }

    @Bean
    public IAuthenticatedUserPort authenticatedUserPort() {
        return new AuthenticatedUserAdapter();
    }

    @Bean
    public ITraceServicePort traceServicePort(ITracePersistencePort tracePersistencePort,
                                              IAuthenticatedUserPort authenticatedUserPort, Clock clock) {
        return new TraceUseCase(tracePersistencePort, authenticatedUserPort, clock);
    }

    @Bean
    public IOrderEfficiencyServicePort orderEfficiencyServicePort(ITracePersistencePort tracePersistencePort,
                                                                  IAuthenticatedUserPort authenticatedUserPort,
                                                                  IRestaurantClientPort restaurantClientPort) {
        return new OrderEfficiencyUseCase(tracePersistencePort, authenticatedUserPort, restaurantClientPort);
    }
}
