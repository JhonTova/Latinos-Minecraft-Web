-- =============================================================================
-- V25 — Direccion de Bedrock (Geyser)
-- =============================================================================
--
-- Marcar la casilla de Bedrock (V23) responde "se puede entrar desde el movil",
-- pero no responde COMO. Y ahi esta el problema real: un cliente de Bedrock no
-- se conecta con una cadena `host:puerto` como el de Java. Pide la IP y el
-- puerto en DOS campos separados, y el puerto por defecto de Bedrock (19132) no
-- es el de Java (25565).
--
-- Sin estos dos datos, el jugador de movil llegaba a la ficha, veia la etiqueta
-- "Bedrock Edition", copiaba la direccion de Java y no podia entrar. La etiqueta
-- prometia algo que la ficha no le daba.
--
-- DOS COLUMNAS Y NO UNA CADENA. Es lo que pide el formulario del cliente, y
-- guardarlo junto obligaria a partirlo al pintarlo — con el riesgo de partir mal
-- una IPv6, que lleva dos puntos de sobra.
--
-- SON NULABLES A PROPOSITO. Los servidores que ya marcaron la casilla no pueden
-- quedarse invalidos por un campo que ayer no existia; siguen saliendo con su
-- etiqueta y sin direccion hasta que su dueño la rellene.
--
-- LO DECLARA EL DUEÑO Y NO SE VERIFICA, igual que la casilla de V23 y la region
-- de V17. No sondeamos Bedrock (RakNet sobre UDP, otro protocolo entero) y, por
-- ADR-0009, un dato declarado nunca alimenta el ranking. Esto solo se enseña.
-- =============================================================================

ALTER TABLE servers
    ADD COLUMN bedrock_host TEXT,
    ADD COLUMN bedrock_port INT;

COMMENT ON COLUMN servers.bedrock_host IS
    'IP o dominio que escriben los jugadores de Bedrock (Geyser). Declarado por el dueño, no verificado. NULL si no lo ha puesto.';

COMMENT ON COLUMN servers.bedrock_port IS
    'Puerto de Geyser. Bedrock lo pide aparte de la IP y por defecto es 19132, no el 25565 de Java.';

-- El rango se comprueba tambien aqui y no solo en la API. Un puerto 0 o 70000
-- no es un dato "un poco raro": es una direccion que no existe, y la ficha la
-- enseñaria igual.
ALTER TABLE servers
    ADD CONSTRAINT servers_puerto_bedrock_valido
        CHECK (bedrock_port IS NULL OR bedrock_port BETWEEN 1 AND 65535);

-- Un puerto sin IP no significa nada, y una IP sin puerto tampoco sirve para
-- conectarse: o estan los dos o no esta ninguno.
ALTER TABLE servers
    ADD CONSTRAINT servers_bedrock_completo
        CHECK ((bedrock_host IS NULL) = (bedrock_port IS NULL));
