-- =============================================================================
-- V6 — Auditoria
-- =============================================================================
-- ADR-0010: se conserva EL HECHO, se anonimiza LA PERSONA.
--
-- Los documentos de requisitos pedian "nunca eliminar registros". Eso es
-- incompatible con el derecho de supresion del RGPD. La solucion no es guardar
-- menos: es separar el hecho auditado (que se conserva) del sujeto (que se
-- seudonimiza cuando vence su plazo).
-- =============================================================================

CREATE TABLE audit_log (
    id                  UUID PRIMARY KEY,

    -- Organizacion afectada. NULL para acciones de plataforma.
    organization_id     UUID REFERENCES organizations (id) ON DELETE SET NULL,

    -- Quien actuo. actor_user_id se pone a NULL al anonimizar; actor_label
    -- conserva una etiqueta no identificativa para que el historico siga
    -- siendo legible ("un administrador", "el sistema").
    actor_type          TEXT NOT NULL,
    actor_user_id       UUID REFERENCES users (id) ON DELETE SET NULL,
    actor_label         TEXT,

    action              TEXT NOT NULL,
    resource_type       TEXT NOT NULL,
    resource_id         UUID,

    -- Contexto del cambio (antes/despues, motivo). Nunca secretos ni claves.
    metadata            JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Seudonimizada desde el principio, nunca en claro (ADR-0010).
    ip_hash             BYTEA,
    user_agent          TEXT,

    -- Enlaza la accion con la peticion HTTP que la origino.
    correlation_id      UUID,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Marca de seudonimizacion ya aplicada. No se revierte.
    pseudonymized_at    TIMESTAMPTZ
);

COMMENT ON TABLE audit_log IS
    'Registro de acciones. Solo INSERT: jamas UPDATE ni DELETE salvo el proceso de seudonimizacion. Retencion: hecho permanente; ip_hash 90 dias; actor seudonimizado a los 24 meses (ADR-0010). Candidata a particionado por created_at.';

CREATE INDEX audit_log_por_organizacion
    ON audit_log (organization_id, created_at DESC);

CREATE INDEX audit_log_por_actor
    ON audit_log (actor_user_id, created_at DESC)
    WHERE actor_user_id IS NOT NULL;

CREATE INDEX audit_log_por_recurso
    ON audit_log (resource_type, resource_id, created_at DESC);

-- Para el proceso periodico de seudonimizacion.
CREATE INDEX audit_log_pendiente_seudonimizar
    ON audit_log (created_at)
    WHERE pseudonymized_at IS NULL AND actor_user_id IS NOT NULL;
