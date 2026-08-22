package com.latinosmc.app.web;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.identity.PlatformAdminRepository;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administracion de la plataforma.
 *
 * <p><strong>Cada metodo comprueba el permiso por separado.</strong> No hay un
 * filtro por prefijo de ruta que lo haga una vez para todo. Es mas repetitivo a
 * proposito: un filtro por ruta protege lo que hay hoy bajo {@code /admin}, y el
 * dia que alguien añada un endpoint administrativo en otro sitio, la proteccion
 * no viaja con el. Aqui, un metodo sin la llamada a {@code exigirAdministrador}
 * se ve leyendolo.
 *
 * <p>Los cambios quedan en {@code audit_log}. Tocar un precio es una accion sobre
 * dinero: sin rastro, un cobro inesperado no se puede explicar.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final PlatformAdminRepository administradores;
    private final BillingRepository facturacion;
    private final com.latinosmc.billing.PatrocinadoresRepository patrocinadores;
    private final com.latinosmc.organizations.SiteSettingsRepository sitio;
    private final com.latinosmc.servers.ProcesadorDeImagenes imagenes;
    private final com.latinosmc.shared.ports.FileStorage almacen;
    private final com.latinosmc.voting.VoteSettingsRepository reglasDeVoto;
    private final com.latinosmc.servers.ServerRepository servidores;

    public AdminController(
            PlatformAdminRepository administradores,
            BillingRepository facturacion,
            com.latinosmc.billing.PatrocinadoresRepository patrocinadores,
            com.latinosmc.voting.VoteSettingsRepository reglasDeVoto,
            com.latinosmc.organizations.SiteSettingsRepository sitio,
            com.latinosmc.servers.ProcesadorDeImagenes imagenes,
            com.latinosmc.servers.ServerRepository servidores,
            com.latinosmc.shared.ports.FileStorage almacen) {
        this.administradores = administradores;
        this.facturacion = facturacion;
        this.patrocinadores = patrocinadores;
        this.reglasDeVoto = reglasDeVoto;
        this.sitio = sitio;
        this.imagenes = imagenes;
        this.servidores = servidores;
        this.almacen = almacen;
    }

    /**
     * Si quien pregunta es administrador.
     *
     * <p>Responde {@code false} en vez de 403 a proposito: lo usa el panel para
     * decidir si enseña el enlace de administracion. Un 403 obligaria a tratar un
     * caso normal —no serlo— como un error.
     *
     * <p>Que responda que si <strong>no</strong> da acceso a nada: cada endpoint
     * vuelve a comprobarlo por su cuenta. Esto solo pinta o esconde un enlace.
     */
    @GetMapping("/soy")
    public Map<String, Boolean> soyAdministrador(@UsuarioActual IdentityRepository.Usuario usuario) {
        return Map.of("administrador", administradores.esAdministrador(usuario.id()));
    }

    @GetMapping("/slots")
    public List<BillingRepository.Slot> slots(@UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);
        return facturacion.slots();
    }

    /**
     * Cambia precio, duracion y disponibilidad de un espacio.
     *
     * <p>No toca lo ya vendido: los periodos reservados guardan sus fechas y cada
     * pedido guarda su importe. Subir el precio hoy no recalcula lo que alguien
     * pago la semana pasada ni acorta un periodo en curso.
     */
    @PutMapping("/slots/{posicion}")
    public BillingRepository.Slot actualizarSlot(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable int posicion,
            @RequestBody @Valid PeticionSlot peticion) {

        exigirAdministrador(usuario);

        var antes = facturacion
                .slot(posicion)
                .orElseThrow(() -> new DomainException.NotFound("slot.not_found", "Ese espacio no existe."));

        facturacion.actualizarSlot(
                posicion,
                peticion.precioCentimos(),
                peticion.diasPeriodo(),
                peticion.maxAdelanto(),
                peticion.activo());

        var despues = facturacion.slot(posicion).orElseThrow();

        administradores.registrar(
                Ids.newId(),
                usuario.id(),
                "featured_slot.updated",
                "featured_slot",
                String.valueOf(posicion),
                Map.of("antes", antes, "despues", despues));

        log.info("Administrador {} cambio el espacio {}", usuario.id(), posicion);
        return despues;
    }

    /** Pedidos de toda la plataforma, del mas reciente al mas antiguo. */
    @GetMapping("/pedidos")
    public List<BillingRepository.Pedido> pedidos(@UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);
        return facturacion.todosLosPedidos(200);
    }

    /** Ingresos netos segun el ledger, no sumando pedidos: los reembolsos restan. */
    @GetMapping("/ingresos")
    public List<BillingRepository.Ingresos> ingresos(@UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);
        return facturacion.ingresosPorMoneda();
    }

    // --- Reglas de voto ------------------------------------------------------

    @GetMapping("/voto")
    public com.latinosmc.voting.VoteSettings reglasDeVoto(@UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);
        return reglasDeVoto.actuales();
    }

    /**
     * Cambia las reglas de voto de toda la plataforma.
     *
     * <p>Es la palanca mas delicada del producto. Activar "un voto al dia" cambia
     * lo que MIDE el ranking: de "cuanta gente juega aqui" a "cual es tu servidor
     * favorito". Y bajar el cupo por IP deja fuera a quien comparte conexion —en
     * Latinoamerica, cualquiera que vote desde datos moviles con CGNAT.
     *
     * <p>Por eso queda registrado con quien lo cambio: un desplome de votos que
     * nadie sabe explicar es lo que convierte un ajuste en un misterio de tres
     * dias.
     */
    @PutMapping("/voto")
    public com.latinosmc.voting.VoteSettings actualizarReglasDeVoto(
            @UsuarioActual IdentityRepository.Usuario usuario, @RequestBody @Valid PeticionReglasDeVoto peticion) {

        exigirAdministrador(usuario);

        var antes = reglasDeVoto.actuales();
        var nuevas = new com.latinosmc.voting.VoteSettings(
                peticion.unVotoAlDia(),
                peticion.maxJugadoresPorIp(),
                java.time.Duration.ofHours(peticion.ventanaHoras()));

        reglasDeVoto.guardar(nuevas, usuario.id());

        administradores.registrar(
                Ids.newId(),
                usuario.id(),
                "vote_settings.updated",
                "vote_settings",
                "global",
                Map.of("antes", antes, "despues", nuevas));

        log.warn("Administrador {} cambio las reglas de voto: {} -> {}", usuario.id(), antes, nuevas);
        return nuevas;
    }

    // --- Textos e imagen del sitio -------------------------------------------

    @GetMapping("/sitio")
    public com.latinosmc.organizations.SiteSettingsRepository.AjustesDelSitio sitio(
            @UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);
        return sitio.actuales();
    }

    /**
     * Cambia el titulo y la descripcion de la portada.
     *
     * <p>Son los textos que decide si alguien pulsa el enlace en Google o en
     * Discord. Poder afinarlos sin desplegar es la diferencia entre probar tres
     * variantes en una tarde y no probar ninguna.
     */
    @PutMapping("/sitio")
    public com.latinosmc.organizations.SiteSettingsRepository.AjustesDelSitio actualizarSitio(
            @UsuarioActual IdentityRepository.Usuario usuario, @RequestBody @Valid PeticionSitio peticion) {

        exigirAdministrador(usuario);

        var antes = sitio.actuales();
        sitio.guardar(
                new com.latinosmc.organizations.SiteSettingsRepository.AjustesDelSitio(
                        peticion.titulo().trim(),
                        peticion.descripcion().trim(),
                        antes.claveOgImage(),
                        peticion.nombre().trim(),
                        antes.claveLogo(),
                        peticion.urlPlugin() == null || peticion.urlPlugin().isBlank()
                                ? null
                                : com.latinosmc.shared.EnlaceExterno.validar(peticion.urlPlugin())),
                usuario.id());

        administradores.registrar(
                Ids.newId(), usuario.id(), "site_settings.updated", "site_settings", "1", Map.of("antes", antes));

        return sitio.actuales();
    }

    /**
     * Sube la imagen que se ve al compartir la portada.
     *
     * <p>Se reprocesa a PNG de 1200x630 antes de guardarla. Reprocesarla no es
     * solo cuestion de tamano: decodificar y volver a codificar destruye
     * cualquier cosa incrustada en el archivo, que es lo que convierte una imagen
     * subida por un tercero en algo seguro de servir (ADR-0017).
     */
    @PostMapping("/sitio/imagen")
    public Map<String, String> subirImagenSocial(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @org.springframework.web.bind.annotation.RequestParam("archivo")
                    org.springframework.web.multipart.MultipartFile archivo) {

        exigirAdministrador(usuario);

        if (archivo == null || archivo.isEmpty()) {
            throw new DomainException.Conflict("image.missing", "No has adjuntado ningun archivo.");
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (java.io.IOException e) {
            throw new DomainException.Conflict("image.unreadable", "No hemos podido leer el archivo.");
        }

        var procesada = imagenes.procesarSocial(bytes);

        // La clave lleva marca de tiempo para que al cambiar la imagen la nueva no
        // quede escondida tras la cache del anterior en Discord y en el navegador.
        String clave = "sitio/og-" + System.currentTimeMillis() + ".png";
        almacen.guardar(clave, procesada.contenido(), procesada.contentType());
        sitio.guardarImagen(clave, usuario.id());

        administradores.registrar(
                Ids.newId(), usuario.id(), "site_settings.image", "site_settings", "1", Map.of("clave", clave));

        return Map.of("url", almacen.urlPublica(clave));
    }

    /**
     * Sube el logotipo del sitio.
     *
     * <p>La clave lleva marca de tiempo para que al cambiarlo el nuevo no quede
     * escondido tras la cache del anterior en el navegador de quien ya visito el
     * sitio.
     */
    @PostMapping("/sitio/logo")
    public Map<String, String> subirLogo(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @org.springframework.web.bind.annotation.RequestParam("archivo")
                    org.springframework.web.multipart.MultipartFile archivo) {

        exigirAdministrador(usuario);

        var procesada = imagenes.procesarLogo(leerArchivo(archivo));

        String clave = "sitio/logo-" + System.currentTimeMillis() + ".png";
        almacen.guardar(clave, procesada.contenido(), procesada.contentType());
        sitio.guardarLogo(clave, usuario.id());

        administradores.registrar(
                Ids.newId(), usuario.id(), "site_settings.logo", "site_settings", "1", Map.of("clave", clave));

        return Map.of("url", almacen.urlPublica(clave));
    }

    private byte[] leerArchivo(org.springframework.web.multipart.MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DomainException.Conflict("image.missing", "No has adjuntado ningun archivo.");
        }
        try {
            return archivo.getBytes();
        } catch (java.io.IOException e) {
            throw new DomainException.Conflict("image.unreadable", "No hemos podido leer el archivo.");
        }
    }

    // --- Patrocinadores de marca ---------------------------------------------

    /** Todos, incluidos los pendientes de aprobar y los ya retirados. */
    @GetMapping("/patrocinadores")
    public List<com.latinosmc.billing.PatrocinadoresRepository.Patrocinador> patrocinadores(
            @UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);
        return patrocinadores.todos();
    }

    /**
     * Da de alta un patrocinador. <strong>Nace sin aprobar.</strong>
     *
     * <p>Aprobar es un segundo acto deliberado incluso para quien lo crea. Un
     * enlace saliente desde nuestro dominio es nuestra reputacion: si apunta a
     * una estafa, el problema no es del que pago, es nuestro. Separar el alta de
     * la publicacion evita que un error de tecleo salga a la portada.
     */
    @PostMapping("/patrocinadores")
    public com.latinosmc.billing.PatrocinadoresRepository.Patrocinador crearPatrocinador(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @RequestBody @Valid PeticionPatrocinador peticion) {

        exigirAdministrador(usuario);

        // La validacion del enlace es lo unico que impide que un href de nuestra
        // portada acabe ejecutando javascript: en el navegador del visitante.
        String url = com.latinosmc.shared.EnlaceExterno.validar(peticion.urlDestino());

        if (!peticion.hasta().isAfter(peticion.desde())) {
            throw new DomainException.Conflict(
                    "sponsor.invalid_period", "La fecha de fin tiene que ser posterior a la de inicio.");
        }

        java.util.UUID id = Ids.newId();
        patrocinadores.crear(
                id,
                peticion.nombre().trim(),
                url,
                peticion.claveLogo(),
                peticion.desde(),
                peticion.hasta(),
                peticion.orden());

        administradores.registrar(
                Ids.newId(),
                usuario.id(),
                "brand_sponsor.created",
                "brand_sponsor",
                id.toString(),
                Map.of("nombre", peticion.nombre(), "url", url));

        return patrocinadores.todos().stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Sube el banner vertical de un patrocinador.
     *
     * <p>Aparte del alta porque hace falta el identificador para nombrar el
     * archivo, y ese no existe hasta que la ficha esta creada.
     *
     * <p>Se reprocesa como cualquier otra imagen subida (ADR-0017). Que la envie
     * un administrador no la hace de fiar: el archivo lo suele mandar la marca
     * por correo, asi que su origen es exactamente el mismo que el de una subida
     * de un desconocido.
     */
    @PostMapping("/patrocinadores/{id}/banner")
    public Map<String, String> subirBannerDePatrocinador(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable java.util.UUID id,
            @org.springframework.web.bind.annotation.RequestParam("archivo")
                    org.springframework.web.multipart.MultipartFile archivo) {

        exigirAdministrador(usuario);

        var procesada = imagenes.procesarBannerVertical(leerArchivo(archivo));

        // La marca de tiempo evita que el banner nuevo quede escondido tras la
        // cache del anterior en el navegador de quien ya visito la portada.
        String extension = "image/gif".equals(procesada.contentType()) ? ".gif" : ".png";
        String clave = "patrocinadores/" + id + "-" + System.currentTimeMillis() + extension;

        almacen.guardar(clave, procesada.contenido(), procesada.contentType());

        if (!patrocinadores.guardarBanner(id, clave)) {
            throw new DomainException.NotFound("sponsor.not_found", "Ese patrocinador no existe.");
        }

        administradores.registrar(
                Ids.newId(),
                usuario.id(),
                "brand_sponsor.banner",
                "brand_sponsor",
                id.toString(),
                Map.of("clave", clave));

        return Map.of("url", almacen.urlPublica(clave));
    }

    @PostMapping("/patrocinadores/{id}/aprobar")
    public Map<String, Boolean> aprobarPatrocinador(
            @UsuarioActual IdentityRepository.Usuario usuario, @PathVariable java.util.UUID id) {

        exigirAdministrador(usuario);
        boolean cambio = patrocinadores.aprobar(id);

        if (cambio) {
            administradores.registrar(
                    Ids.newId(), usuario.id(), "brand_sponsor.approved", "brand_sponsor", id.toString(), Map.of());
            log.info("Administrador {} aprobo el patrocinador {}", usuario.id(), id);
        }
        return Map.of("aprobado", cambio);
    }

    @PostMapping("/patrocinadores/{id}/retirar")
    public Map<String, Boolean> retirarPatrocinador(
            @UsuarioActual IdentityRepository.Usuario usuario, @PathVariable java.util.UUID id) {

        exigirAdministrador(usuario);
        boolean cambio = patrocinadores.cancelar(id);

        if (cambio) {
            administradores.registrar(
                    Ids.newId(), usuario.id(), "brand_sponsor.cancelled", "brand_sponsor", id.toString(), Map.of());
        }
        return Map.of("retirado", cambio);
    }

    @GetMapping("/administradores")
    public List<PlatformAdminRepository.Administrador> administradores(
            @UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);
        return administradores.administradores();
    }

    // --- Moderacion de servidores (ADR-0028) ---------------------------------

    /** Todos los servidores vivos, con los suspendidos primero. */
    @GetMapping("/servidores")
    public List<ServidorAdministrable> servidores(@UsuarioActual IdentityRepository.Usuario usuario) {
        exigirAdministrador(usuario);

        return servidores.paraAdministracion(300).stream()
                .map(s -> new ServidorAdministrable(
                        s.id(),
                        s.slug(),
                        s.nombre(),
                        s.estado(),
                        s.motivoDeSuspension(),
                        s.claveIcono() == null ? null : almacen.urlPublica(s.claveIcono()),
                        s.votosSemana()))
                .toList();
    }

    /**
     * Suspende un servidor (ADR-0028).
     *
     * <p>Desaparece del directorio, del ranking, de la busqueda, del sondeo y deja
     * de aceptar votos. Sus datos no se tocan: una suspension puede levantarse, y
     * al levantarla el servidor vuelve exactamente como estaba.
     *
     * <p><strong>No caduca y no se purga sola.</strong> Es deliberado: un
     * temporizador de borrado sobre la suspension destruiria la prueba de por que
     * suspendimos a alguien justo cuando mas falta hace, y le bastaria con esperar
     * para que su historial se limpiara solo.
     *
     * <p>El motivo es obligatorio y lo ve el dueño en su panel. Un servidor que
     * desaparece sin explicacion es un ticket de soporte garantizado y la
     * sensacion de que esto es arbitrario.
     */
    @PostMapping("/servidores/{servidorId}/suspender")
    public Map<String, Object> suspender(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable java.util.UUID servidorId,
            @RequestBody @Valid PeticionSuspension peticion) {

        exigirAdministrador(usuario);

        String motivo = peticion.motivo().trim();
        int filas = servidores.suspender(servidorId, motivo, usuario.id());

        administradores.registrar(
                Ids.newId(),
                usuario.id(),
                "server.suspended",
                "server",
                servidorId.toString(),
                Map.of("motivo", motivo));

        log.warn("El administrador {} suspendio el servidor {}: {}", usuario.id(), servidorId, motivo);
        return Map.of("suspendido", filas > 0);
    }

    @PostMapping("/servidores/{servidorId}/levantar-suspension")
    public Map<String, Object> levantarSuspension(
            @UsuarioActual IdentityRepository.Usuario usuario, @PathVariable java.util.UUID servidorId) {

        exigirAdministrador(usuario);
        int filas = servidores.levantarSuspension(servidorId);

        administradores.registrar(
                Ids.newId(),
                usuario.id(),
                "server.unsuspended",
                "server",
                servidorId.toString(),
                Map.of());

        log.warn("El administrador {} levanto la suspension del servidor {}", usuario.id(), servidorId);
        return Map.of("activo", filas > 0);
    }

    /**
     * @param motivo obligatorio: lo ve el dueño y queda en la auditoria. Sin el,
     *     una suspension es indistinguible de un fallo de la plataforma
     */
    public record PeticionSuspension(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 500)
                    String motivo) {}

    public record ServidorAdministrable(
            java.util.UUID id,
            String slug,
            String nombre,
            String estado,
            String motivoDeSuspension,
            String iconoUrl,
            long votosSemana) {}

    /**
     * Servidores cuyas imagenes hemos cogido de su ping, para revisarlas.
     *
     * <p>Es la pantalla que hace viable la politica del ADR-0027: adoptamos
     * contenido de terceros al instante y lo servimos desde nuestro dominio, asi
     * que tiene que haber un sitio donde verlo y un boton para quitarlo. Sin eso,
     * "publicar y poder retirar" seria solo "publicar".
     */
    @GetMapping("/servidores/imagenes-automaticas")
    public List<ServidorConImagenAutomatica> imagenesAutomaticas(
            @UsuarioActual IdentityRepository.Usuario usuario) {

        exigirAdministrador(usuario);

        return servidores.conImagenesAutomaticas(200).stream()
                .map(s -> new ServidorConImagenAutomatica(
                        s.id(),
                        s.slug(),
                        s.nombre(),
                        s.motdJson(),
                        s.claveIconoAuto() == null ? null : almacen.urlPublica(s.claveIconoAuto()),
                        s.claveBannerAuto() == null ? null : almacen.urlPublica(s.claveBannerAuto()),
                        s.bloqueado(),
                        s.motivoDelBloqueo(),
                        s.actualizadoEn()))
                .toList();
    }

    public record ServidorConImagenAutomatica(
            java.util.UUID id,
            String slug,
            String nombre,
            String motd,
            String iconoUrl,
            String bannerUrl,
            boolean bloqueado,
            String motivoDelBloqueo,
            java.time.Instant actualizadoEn) {}

    /**
     * Retira las imagenes automaticas de un servidor (ADR-0027).
     *
     * <p>Es la contrapartida de adoptar contenido de terceros al instante. Alguien
     * pone algo que no debe de icono o de MOTD y esto lo quita: el servidor vuelve
     * al marcador de posicion y no se le readopta nada mas, aunque cambie el
     * favicon al minuto siguiente.
     *
     * <p>No suspende el servidor ni le quita los votos. Es una medida sobre las
     * imagenes, no sobre el servidor; para lo otro esta la suspension.
     */
    @PostMapping("/servidores/{servidorId}/imagenes-automaticas/bloquear")
    public Map<String, Object> bloquearImagenesAutomaticas(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable java.util.UUID servidorId,
            @RequestBody(required = false) PeticionBloqueo peticion) {

        exigirAdministrador(usuario);

        String motivo = peticion == null || peticion.motivo() == null || peticion.motivo().isBlank()
                ? "Retirado por un administrador"
                : peticion.motivo().trim();

        int filas = servidores.bloquearImagenesAutomaticas(servidorId, motivo.substring(0, Math.min(motivo.length(), 300)));

        // Se registra siempre: es una accion de moderacion sobre contenido ajeno y
        // tiene que poder reconstruirse quien la tomo y cuando.
        log.warn(
                "El administrador {} bloqueo las imagenes automaticas del servidor {}: {}",
                usuario.id(),
                servidorId,
                motivo);

        return Map.of("bloqueado", filas > 0);
    }

    @PostMapping("/servidores/{servidorId}/imagenes-automaticas/desbloquear")
    public Map<String, Object> desbloquearImagenesAutomaticas(
            @UsuarioActual IdentityRepository.Usuario usuario, @PathVariable java.util.UUID servidorId) {

        exigirAdministrador(usuario);
        int filas = servidores.desbloquearImagenesAutomaticas(servidorId);
        log.warn(
                "El administrador {} desbloqueo las imagenes automaticas del servidor {}",
                usuario.id(),
                servidorId);

        return Map.of("desbloqueado", filas > 0);
    }

    public record PeticionBloqueo(String motivo) {}

    private void exigirAdministrador(IdentityRepository.Usuario usuario) {
        if (!administradores.esAdministrador(usuario.id())) {
            // Se registra: alguien con sesion valida llamando a administracion es
            // informacion util, sea un error de navegacion o un tanteo.
            log.warn("Acceso a administracion denegado al usuario {}", usuario.id());
            throw new DomainException.Forbidden(
                    "admin.required", "Esta zona es solo para administradores de la plataforma.");
        }
    }

    /**
     * @param precioCentimos siempre entero. Cero significa gratis, no "desactivado":
     *     para desactivar esta {@code activo}.
     * @param diasPeriodo entre 1 y 90. Vender un periodo de un año equivale a
     *     cobrar por adelantado un servicio muy lejano, con el mismo riesgo de
     *     reembolsos que llevo a descartar el wallet (ADR-0013).
     * @param maxAdelanto cuantos periodos se pueden vender por adelantado
     */
    public record PeticionSlot(
            @Min(0) @Max(100_000_00L) long precioCentimos,
            @Min(1) @Max(90) int diasPeriodo,
            @Min(1) @Max(12) int maxAdelanto,
            boolean activo) {}

    /**
     * @param maxJugadoresPorIp cuantos jugadores distintos pueden votar desde la
     *     misma conexion al dia. El maximo de 500 no es capricho: por encima de
     *     eso la regla deja de filtrar nada y es mas honesto desactivarla.
     * @param ventanaHoras 24 es un dia; el tope de una semana evita que un cero
     *     mal tecleado deje a todo el mundo sin poder votar durante meses.
     */
    public record PeticionReglasDeVoto(
            boolean unVotoAlDia,
            @Min(1) @Max(500) int maxJugadoresPorIp,
            @Min(1) @Max(168) int ventanaHoras) {}

    /**
     * @param urlDestino se valida con {@link com.latinosmc.shared.EnlaceExterno}
     *     antes de tocar la base de datos. Es lo unico que impide que un
     *     {@code href} de nuestra portada acabe ejecutando codigo.
     * @param claveLogo clave de una imagen ya subida, o null mientras no la haya
     * @param orden menor numero, aparece antes
     */
    /**
     * @param titulo lo que se ve en la pestaña, en Google y en la tarjeta de
     *     Discord. Google corta alrededor de los 60 caracteres.
     * @param descripcion entre 120 y 160 caracteres funciona mejor: por debajo
     *     desaprovecha espacio, por encima Google la corta a mitad de frase.
     */
    public record PeticionSitio(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 3, max = 70)
                    String titulo,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 20, max = 300)
                    String descripcion,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 40) String nombre,
            @jakarta.validation.constraints.Size(max = 512) String urlPlugin) {}

    public record PeticionPatrocinador(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String nombre,
            @jakarta.validation.constraints.NotBlank String urlDestino,
            @jakarta.validation.constraints.Size(max = 200) String claveLogo,
            @jakarta.validation.constraints.NotNull java.time.Instant desde,
            @jakarta.validation.constraints.NotNull java.time.Instant hasta,
            @Min(0) @Max(1000) int orden) {}
}
