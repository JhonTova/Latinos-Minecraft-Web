package com.latinosmc.organizations;

import com.latinosmc.shared.Ids;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a los datos de organizaciones. Solo el modulo {@code organizations} toca estas tablas. */
@Repository
public class OrganizationRepository {

    private final JdbcClient jdbc;

    public OrganizationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> buscarIdPorSlug(String slug) {
        return jdbc.sql("SELECT id FROM organizations WHERE slug = :slug AND deleted_at IS NULL")
                .param("slug", slug)
                .query(UUID.class)
                .optional();
    }

    /**
     * Crea la organizacion personal de un usuario recien registrado y lo deja como
     * OWNER.
     *
     * <p>Todo dueno tiene organizacion desde el primer momento, aunque trabaje
     * solo. Asi, invitar a alguien mas adelante es anadir un miembro y no migrar
     * la estructura entera — que es exactamente lo que cuesta meses cuando ya hay
     * datos reales.
     */
    public UUID crearOrganizacionPersonal(UUID usuarioId, String nombreUsuario) {
        UUID id = Ids.newId();
        String slug = slugLibre(nombreUsuario);

        jdbc.sql("INSERT INTO organizations (id, slug, name) VALUES (:id, :slug, :nombre)")
                .param("id", id)
                .param("slug", slug)
                .param("nombre", nombreUsuario)
                .update();

        jdbc.sql(
                        """
                        INSERT INTO organization_members (organization_id, user_id, role_code, joined_at)
                        VALUES (:org, :usuario, 'OWNER', now())
                        """)
                .param("org", id)
                .param("usuario", usuarioId)
                .update();

        return id;
    }

    /** Organizaciones a las que pertenece un usuario, con su rol. */
    public List<Membresia> organizacionesDe(UUID usuarioId) {
        return jdbc.sql(
                        """
                        SELECT o.id, o.slug, o.name, o.plan_code, m.role_code
                          FROM organization_members m
                          JOIN organizations o ON o.id = m.organization_id
                         WHERE m.user_id = :usuario
                           AND o.deleted_at IS NULL
                           AND (m.expires_at IS NULL OR m.expires_at > now())
                         ORDER BY o.name
                        """)
                .param("usuario", usuarioId)
                .query((rs, fila) -> new Membresia(
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("plan_code"),
                        rs.getString("role_code")))
                .list();
    }

    /**
     * Comprueba que el usuario puede gestionar la organizacion.
     *
     * <p>Es la barrera de aislamiento multi-tenant: sin esta comprobacion,
     * cualquier usuario autenticado podria operar sobre servidores ajenos pasando
     * otro identificador de organizacion. Es el fallo que OWASP llama BOLA y el
     * numero uno en APIs multi-tenant.
     */
    public boolean puedeGestionar(UUID usuarioId, UUID organizacionId) {
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(*)
                          FROM organization_members m
                          JOIN organization_roles r ON r.code = m.role_code
                         WHERE m.user_id = :usuario
                           AND m.organization_id = :org
                           AND r.rank <= 2
                           AND (m.expires_at IS NULL OR m.expires_at > now())
                        """)
                .param("usuario", usuarioId)
                .param("org", organizacionId)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    /**
     * Quien puede gestionar la organizacion, para avisarles de algo.
     *
     * <p>Devuelve identificadores de usuario, no correos. El correo vive en el
     * modulo de identidad y se pide alli: unir con {@code users} desde aqui
     * seria leer la tabla de otro modulo, que es justo lo que la regla 9 impide
     * y lo que ADR-0019 ya registra suficientes veces.
     */
    public List<UUID> administradoresDe(UUID organizacionId) {
        return jdbc.sql(
                        """
                        SELECT m.user_id
                          FROM organization_members m
                          JOIN organization_roles r ON r.code = m.role_code
                         WHERE m.organization_id = :org
                           AND r.rank <= 2
                           AND (m.expires_at IS NULL OR m.expires_at > now())
                        """)
                .param("org", organizacionId)
                .query(UUID.class)
                .list();
    }

    /** Limite de servidores del plan. {@code null} significa sin limite. */
    public Integer limiteDeServidores(UUID organizacionId) {
        return jdbc.sql(
                        """
                        SELECT p.max_servers
                          FROM organizations o
                          JOIN plans p ON p.code = o.plan_code
                         WHERE o.id = :org
                        """)
                .param("org", organizacionId)
                .query(Integer.class)
                .optional()
                .orElse(0);
    }

    private String slugLibre(String base) {
        String raiz = base.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (raiz.isBlank()) {
            raiz = "org";
        }
        String candidato = raiz;
        int sufijo = 2;
        while (buscarIdPorSlug(candidato).isPresent()) {
            candidato = raiz + "-" + sufijo++;
        }
        return candidato;
    }

    public record Membresia(UUID id, String slug, String nombre, String plan, String rol) {}

    /** Crea la organizacion si no existe y devuelve su id en cualquier caso. */
    public UUID crearSiNoExiste(String slug, String nombre) {
        return buscarIdPorSlug(slug).orElseGet(() -> {
            UUID id = Ids.newId();
            jdbc.sql("INSERT INTO organizations (id, slug, name) VALUES (:id, :slug, :nombre)")
                    .param("id", id)
                    .param("slug", slug)
                    .param("nombre", nombre)
                    .update();
            return id;
        });
    }
}
