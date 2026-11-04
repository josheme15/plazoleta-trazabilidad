package com.plazoleta.trazabilidad.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedUserAdapterTest {

    private final AuthenticatedUserAdapter adapter = new AuthenticatedUserAdapter();

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsDataOfTheEmployeeToken() {
        authenticate(new AuthenticatedUser(8L, "EMPLOYEE", 1L));

        assertEquals(8L, adapter.getAuthenticatedUserId());
        assertEquals("EMPLOYEE", adapter.getAuthenticatedRole());
        assertEquals(1L, adapter.getAuthenticatedRestaurantId());
    }

    @Test
    void clientHasNoRestaurant() {
        authenticate(new AuthenticatedUser(30L, "CLIENT", null));

        assertNull(adapter.getAuthenticatedRestaurantId());
    }

    @Test
    void tokenWithoutId_throwsException() {
        authenticate(new AuthenticatedUser(null, "CLIENT", null));

        assertThrows(AuthenticationCredentialsNotFoundException.class, adapter::getAuthenticatedUserId);
    }

    @Test
    void withoutAuthentication_throwsException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, adapter::getAuthenticatedRole);
    }

    @Test
    void withAnotherPrincipalType_throwsException() {
        authenticate("usuario");

        assertThrows(AuthenticationCredentialsNotFoundException.class, adapter::getAuthenticatedRole);
    }
}
