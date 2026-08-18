package com.latinosmc.app.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta el usuario de la sesion en un metodo de controlador.
 *
 * <p>Si el parametro no es {@code Optional}, la peticion sin sesion valida se
 * rechaza automaticamente con 401. Es deliberado: asi proteger un endpoint es
 * declarar el parametro, y <em>olvidarse</em> de protegerlo requiere un acto
 * consciente en vez de un despiste.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioActual {}
