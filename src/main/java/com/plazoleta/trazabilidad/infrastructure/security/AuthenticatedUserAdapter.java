package com.plazoleta.trazabilidad.infrastructure.security;

import com.plazoleta.trazabilidad.domain.spi.IAuthenticatedUserPort;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Obtiene los datos del usuario autenticado desde el contexto de seguridad (lo carga JwtAuthenticationFilter).
 */
public class AuthenticatedUserAdapter implements IAuthenticatedUserPort {

    @Override
    public Long getAuthenticatedUserId() {
        Long id = currentUser().id();
        if (id == null) {
            throw new AuthenticationCredentialsNotFoundException("El token no tiene el id del usuario");
        }
        return id;
    }

    @Override
    public String getAuthenticatedRole() {
        return currentUser().role();
    }

    @Override
    public Long getAuthenticatedRestaurantId() {
        return currentUser().restaurantId();
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado en la peticion");
    }
}
