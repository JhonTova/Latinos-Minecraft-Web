-- =============================================================================
-- V19 — La ventana de voto deja de ser de cada servidor
-- =============================================================================
--
-- Hasta ahora cada servidor elegia cada cuantas horas podia votar el mismo
-- jugador, entre 12 y 48. Lo elegia su dueño, al registrarlo.
--
-- Eso es un incentivo puesto del reves. Un servidor con la ventana en 12 horas
-- recibe el DOBLE de votos que uno identico con la ventana en 24, sin que sus
-- jugadores hagan nada distinto. El ranking dejaba de comparar servidores y
-- pasaba a comparar quien habia rellenado mejor el formulario.
--
-- No hacia falta mala fe: basta con que uno lo baje para que a todos los demas
-- les convenga bajarlo tambien. Y el producto es la credibilidad del ranking.
--
-- La ventana pasa a ser una sola para toda la plataforma, en `vote_settings`
-- (V14), donde ya vivia la ventana global y donde solo llega el administrador
-- de la plataforma. Por defecto, 24 horas.
--
-- Se BORRA la columna en lugar de dejarla sin usar. Una columna que nadie lee
-- pero sigue ahi es una invitacion a volver a conectarla dentro de seis meses
-- sin recordar por que se desconecto.
--
-- Ver ADR-0023.
-- =============================================================================

ALTER TABLE servers DROP CONSTRAINT IF EXISTS servers_ventana_valida;

ALTER TABLE servers DROP COLUMN IF EXISTS vote_window_hours;

COMMENT ON TABLE vote_settings IS
    'Reglas de voto de toda la plataforma, incluida la ventana entre votos. '
    'Editables desde administracion sin desplegar. Ningun dueño de servidor las toca.';
