package com.latinosmc.app.web;

import com.latinosmc.identity.AuthService;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.shared.DomainException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resuelve {@link UsuarioActual} leyendo la cookie de sesion. */
@Component
public class UsuarioActualResolver implements HandlerMethodArgumentResolver {

    public static final String COOKIE_SESION = "lmc_sesion";

    private final AuthService auth;

    public UsuarioActualResolver(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.hasParameterAnnotation(UsuarioActual.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parametro,
            ModelAndViewContainer mav,
            NativeWebRequest peticionWeb,
            WebDataBinderFactory binder) {

        HttpServletRequest peticion = peticionWeb.getNativeRequest(HttpServletRequest.class);
        Optional<IdentityRepository.Usuario> usuario = auth.usuarioDe(tokenDe(peticion));

        // Un parametro Optional acepta anonimos; uno directo exige sesion.
        if (Optional.class.isAssignableFrom(parametro.getParameterType())) {
            return usuario;
        }
        return usuario.orElseThrow(() -> new NoAutenticado());
    }

    private String tokenDe(HttpServletRequest peticion) {
        if (peticion == null || peticion.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : peticion.getCookies()) {
            if (COOKIE_SESION.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** Falta sesion o ha caducado. Se traduce a 401 en el manejador de errores. */
    public static class NoAutenticado extends DomainException {
        public NoAutenticado() {
            super("auth.required", "Necesitas iniciar sesion.");
        }
    }
}
