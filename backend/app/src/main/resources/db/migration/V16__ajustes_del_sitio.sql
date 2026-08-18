-- =============================================================================
-- V16 — Textos e imagen del sitio, editables sin desplegar
-- =============================================================================
--
-- El titulo, la descripcion y la imagen que se ve al compartir un enlace en
-- Discord o WhatsApp estaban escritos en el codigo. Cambiar una coma exigia un
-- despliegue, y son exactamente las cosas que mas se retocan: el texto que
-- decide si alguien pulsa o pasa de largo se afina probando.
--
-- Fila unica, como vote_settings. No es una tabla de "configuracion generica"
-- clave-valor a proposito: con columnas tipadas, la base de datos garantiza que
-- 'og_image_key' es texto y no un numero, y el codigo que lo lee no tiene que
-- comprobar nada.

CREATE TABLE site_settings (
    id                  BOOLEAN PRIMARY KEY DEFAULT TRUE,

    -- Titulo de la portada. Sale en la pestaña del navegador, en Google y en la
    -- tarjeta de Discord.
    site_title          TEXT NOT NULL,

    -- Descripcion de la portada. Google la enseña bajo el titulo y las redes la
    -- ponen bajo la imagen. Entre 120 y 160 caracteres: por debajo desaprovecha
    -- espacio, por encima Google la corta a mitad de frase.
    site_description    TEXT NOT NULL,

    -- Imagen que se ve al compartir la portada. 1200x630 es el tamaño que
    -- esperan Discord, WhatsApp, Twitter y Facebook.
    og_image_key        TEXT,

    -- Nombre corto de la marca, para el sufijo de los titulos internos.
    site_name           TEXT NOT NULL DEFAULT 'LatinosMC',

    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID REFERENCES users (id) ON DELETE SET NULL,

    CONSTRAINT site_settings_fila_unica CHECK (id IS TRUE),
    CONSTRAINT site_settings_titulo_util CHECK (length(site_title) BETWEEN 3 AND 70),
    CONSTRAINT site_settings_descripcion_util CHECK (length(site_description) BETWEEN 20 AND 300)
);

-- Los valores de partida ya vienen escritos para buscadores.
--
-- La gente no busca "LatinosMC": busca lo que quiere. "servidores de minecraft
-- en español", "servidores minecraft latinos", "servidores hispanos". El titulo
-- lleva esas palabras porque es lo que se compara con lo que la gente escribe;
-- la marca va al final, cuando ya se ha ganado el clic.
INSERT INTO site_settings (id, site_title, site_description) VALUES (
    TRUE,
    'Servidores de Minecraft en español — Ranking de servidores latinos',
    'Los mejores servidores de Minecraft en español y latinos. Ranking honesto con votos verificados: encuentra survival, skyblock, factions y más, con IP para entrar al momento.'
);

COMMENT ON TABLE site_settings IS
    'Titulo, descripcion e imagen social de la portada. Editables desde administracion sin desplegar.';
