-- =============================================================================
-- V24 — Seis espacios destacados y banner vertical de marca
-- =============================================================================
--
-- Dos cambios que van juntos porque los dos son del bloque de publicidad de la
-- portada, y separarlos habria dejado media portada apuntando a columnas que la
-- otra mitad todavia no tiene.
--
-- 1. SEIS ESPACIOS EN LUGAR DE CINCO
--
--    Cinco en una rejilla de dos por fila deja la ultima fila coja: dos, dos y
--    uno. El hueco suelto se lee como un error de maquetacion, no como un
--    espacio libre, y justo ese es el que hay que vender.
--
--    El sexto entra al final de la escala de precios. No abarata los cinco de
--    arriba: cada posicion conserva el suyo y lo ya vendido no se toca (los
--    periodos guardan sus fechas y los pedidos su importe).
--
-- 2. BANNER VERTICAL PARA LAS MARCAS
--
--    Un patrocinador de marca solo tenia logotipo, y un logotipo de 36 px en una
--    fila de tarjetas no es un espacio publicitario: es una nota al pie. El
--    banner vertical se pinta en los laterales de la portada, que es sitio que
--    hasta ahora se desperdiciaba.
--
--    Es NULL para los que ya existen: quien no lo suba sigue saliendo con su
--    logotipo y su nombre. Nada deja de verse por no tener banner.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Sexto espacio destacado
-- -----------------------------------------------------------------------------
ALTER TABLE featured_slots
    DROP CONSTRAINT featured_slots_posicion_valida;

ALTER TABLE featured_slots
    ADD CONSTRAINT featured_slots_posicion_valida CHECK (position BETWEEN 1 AND 6);

-- ON CONFLICT y no un INSERT a secas: esta migracion tiene que poder correr
-- sobre una base donde alguien ya haya creado la fila a mano.
INSERT INTO featured_slots (position, price_cents) VALUES (6, 900)
ON CONFLICT (position) DO NOTHING;

COMMENT ON TABLE featured_slots IS
    'Precios de los 6 espacios destacados. Editables desde administracion sin desplegar.';

-- -----------------------------------------------------------------------------
-- Banner vertical de los patrocinadores de marca
-- -----------------------------------------------------------------------------
-- Clave en el almacen, no una URL: la URL publica depende de donde este el
-- almacen (MinIO en local, R2 en produccion) y la compone quien pinta.
ALTER TABLE brand_sponsors
    ADD COLUMN banner_key TEXT;

COMMENT ON COLUMN brand_sponsors.banner_key IS
    'Banner vertical para los laterales de la portada. NULL: se pinta el logotipo y el nombre.';

-- -----------------------------------------------------------------------------
-- "Ranking honesto" -> "Ranking"
-- -----------------------------------------------------------------------------
-- Presumir de honestidad en la descripcion del sitio consigue lo contrario de
-- lo que busca: quien lo lee se pregunta por que hace falta decirlo. El ranking
-- se demuestra con votos verificados, no con un adjetivo.
--
-- Solo se toca si el texto sigue siendo el que sembramos en V16. Si alguien lo
-- reescribio desde administracion, manda lo suyo.
UPDATE site_settings
   SET site_description = replace(site_description, 'Ranking honesto', 'Ranking')
 WHERE site_description LIKE '%Ranking honesto%';
