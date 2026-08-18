package com.latinosmc.app.web;

import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.RegisterServerService;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/servers")
public class ServerController {

    private final RegisterServerService registro;
    private final OrganizationRepository organizaciones;
    private final ServerRepository servidores;
    private final com.latinosmc.identity.VerificationRepository verificacion;
    private final com.latinosmc.shared.ports.FileStorage almacenArchivos;
    private final com.latinosmc.billing.BillingRepository facturacion;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ServerController.class);

    public ServerController(
            RegisterServerService registro,
            OrganizationRepository organizaciones,
            ServerRepository servidores,
            com.latinosmc.identity.VerificationRepository verificacion,
            com.latinosmc.shared.ports.FileStorage almacenArchivos,
            com.latinosmc.billing.BillingRepository facturacion) {
        this.registro = registro;
        this.organizaciones = organizaciones;
        this.servidores = servidores;
        this.verificacion = verificacion;
        this.almacenArchivos = almacenArchivos;
        this.facturacion = facturacion;
    }

    /** URL pública de una imagen, o null si el servidor no tiene ninguna. */
    private String urlDe(String clave) {
        return clave == null ? null : almacenArchivos.urlPublica(clave);
    }

    /**
     * Registra un servidor y devuelve la clave del plugin.
     *
     * <p>Exige sesion: el parametro {@link UsuarioActual} sin {@code Optional}
     * hace que una peticion anonima se rechace con 401 antes de llegar aqui.
     *
     * <p>La organizacion NO viene en el cuerpo de la peticion. Viene de las
     * membresias del usuario autenticado y se comprueba que puede gestionarla. Si
     * se aceptara del cuerpo, cualquiera podria crear servidores dentro de la
     * organizacion de otro simplemente escribiendo su identificador.
     *
     * <p>La clave se muestra <strong>una sola vez</strong>: en la base solo vive
     * su hash. El servidor queda en {@code PENDING_VERIFICATION} hasta que el
     * plugin se conecte, porque esa conexion es la prueba de propiedad.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<RespuestaRegistro> registrar(
            @UsuarioActual IdentityRepository.Usuario usuario, @RequestBody @Valid PeticionRegistro peticion) {

        // Aqui es donde la verificacion de correo importa de verdad. Si cualquiera
        // pudiera registrar servidores con un correo inventado, montar una granja
        // de servidores falsos para inflar el ranking costaria cero.
        if (!verificacion.tieneEmailVerificado(usuario.id())) {
            throw new DomainException.Conflict(
                    "email.not_verified",
                    "Verifica tu correo antes de registrar un servidor. Te enviamos un codigo al crear la cuenta.");
        }

        UUID organizacionId = organizacionDe(usuario, peticion.organizacionId());

        Integer limite = organizaciones.limiteDeServidores(organizacionId);
        if (limite != null && servidores.contarDe(organizacionId) >= limite) {
            throw new DomainException.Conflict(
                    "plan.server_limit",
                    "Tu plan permite " + limite + " servidores. Amplia a Plus para tener servidores ilimitados.");
        }

        var resultado = registro.registrar(new RegisterServerService.Peticion(
                organizacionId,
                peticion.slug(),
                peticion.nombre(),
                peticion.descripcion(),
                peticion.host(),
                peticion.puerto(),
                peticion.aceptaOffline()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RespuestaRegistro(
                        resultado.servidorId(),
                        resultado.slug(),
                        resultado.claveApi(),
                        "Guarda esta clave: no se vuelve a mostrar. Ponla en el config.yml del plugin."));
    }

    /** Servidores del usuario, agrupados por organizacion. */
    @GetMapping("/mios")
    public List<ServidorPropio> mios(@UsuarioActual IdentityRepository.Usuario usuario) {
        return organizaciones.organizacionesDe(usuario.id()).stream()
                .flatMap(org -> servidores.listarDe(org.id()).stream()
                        .map(s -> new ServidorPropio(
                                s.id(), s.slug(), s.nombre(), s.descripcion(), s.estado(), org.nombre(), org.plan())))
                .toList();
    }

    /**
     * Ficha de un servidor propio.
     *
     * <p>Mismo error para "no existe" y "no es tuyo": distinguirlos permitiria
     * averiguar que servidores hay en la plataforma probando identificadores.
     */
    @GetMapping("/{servidorId}")
    public FichaServidor ficha(
            @UsuarioActual IdentityRepository.Usuario usuario, @PathVariable UUID servidorId) {

        var detalle = servidores
                .detalle(servidorId)
                .filter(d -> organizaciones.puedeGestionar(usuario.id(), d.organizacionId()))
                .orElseThrow(() -> new DomainException.NotFound(
                        "server.not_found", "Ese servidor no existe o no puedes gestionarlo."));

        return new FichaServidor(
                detalle.id(),
                detalle.slug(),
                detalle.nombre(),
                detalle.descripcion(),
                detalle.estado(),
                detalle.host(),
                detalle.puerto(),
                detalle.aceptaOffline(),
                detalle.verificado(),
                urlDe(detalle.claveIcono()),
                urlDe(detalle.claveBanner()),
                detalle.versionMinima(),
                detalle.versionMaxima(),
                detalle.region(),
                detalle.tema(),
                servidores.modalidadesDe(detalle.id()).stream()
                        .map(com.latinosmc.servers.ServerRepository.Modalidad::codigo)
                        .toList());
    }

    /** Catalogo de modalidades. Publico: es una lista fija, no revela nada. */
    @GetMapping("/modalidades")
    public List<com.latinosmc.servers.ServerRepository.Modalidad> modalidades() {
        return servidores.modalidades();
    }

    /**
     * Edita la ficha de un servidor propio.
     *
     * <p>No deja tocar el slug ni la direccion. El slug es la URL publica y
     * cambiarlo rompe los enlaces que ya circulan por Discord; la direccion es lo
     * que medimos por ping, y cambiarla libremente permitiria apuntar el ping a
     * un servidor ajeno con mas jugadores que el propio.
     */
    @org.springframework.web.bind.annotation.PutMapping("/{servidorId}")
    public FichaServidor actualizar(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable UUID servidorId,
            @RequestBody @jakarta.validation.Valid PeticionFicha peticion) {

        UUID organizacionId = servidores
                .organizacionDe(servidorId)
                .filter(org -> organizaciones.puedeGestionar(usuario.id(), org))
                .orElseThrow(() -> new DomainException.NotFound(
                        "server.not_found", "Ese servidor no existe o no puedes gestionarlo."));

        servidores.actualizarFicha(
                servidorId,
                organizacionId,
                recortar(peticion.descripcion(), 500),
                recortar(peticion.versionMinima(), 12),
                recortar(peticion.versionMaxima(), 12),
                recortar(peticion.region(), 40),
                peticion.tema() == null || peticion.tema().isBlank() ? "DEFECTO" : peticion.tema());

        servidores.reemplazarModalidades(
                servidorId, peticion.modalidades() == null ? List.of() : peticion.modalidades());

        return ficha(usuario, servidorId);
    }

    /**
     * Da de baja un servidor.
     *
     * <p><strong>Exige escribir el nombre exacto.</strong> Y se comprueba AQUI,
     * no en el navegador: una confirmacion que solo vive en el cliente es teatro
     * — basta con llamar a la API directamente para saltarsela. Lo que hace que
     * sirva de algo es que el servidor la verifique.
     *
     * <p>Se compara sin distinguir mayusculas ni espacios de los extremos. La
     * confirmacion existe para obligar a mirar QUE se esta borrando, no para
     * jugar a las adivinanzas con el teclado.
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/{servidorId}")
    public java.util.Map<String, Boolean> darDeBaja(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable UUID servidorId,
            @RequestBody @jakarta.validation.Valid PeticionBaja peticion) {

        var detalle = servidores
                .detalle(servidorId)
                .filter(d -> organizaciones.puedeGestionar(usuario.id(), d.organizacionId()))
                .orElseThrow(() -> new DomainException.NotFound(
                        "server.not_found", "Ese servidor no existe o no puedes gestionarlo."));

        if (!detalle.nombre().trim().equalsIgnoreCase(peticion.nombre().trim())) {
            throw new DomainException.Conflict(
                    "server.name_mismatch", "El nombre no coincide. Escríbelo tal y como aparece arriba.");
        }

        // Un espacio destacado en curso es dinero ya pagado. Darlo de baja lo
        // tiraria a la basura sin avisar, y esa llamada la acabamos atendiendo
        // nosotros.
        facturacion
                .destacadoVigenteDe(servidorId, java.time.Instant.now())
                .ifPresent(hasta -> {
                    throw new DomainException.Conflict(
                            "server.featured_active",
                            "Este servidor tiene un espacio destacado pagado hasta el "
                                    + java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM")
                                            .withZone(java.time.ZoneId.of("Europe/Madrid"))
                                            .withLocale(new java.util.Locale("es", "ES"))
                                            .format(hasta)
                                    + ". Escríbenos si aun así quieres darlo de baja.");
                });

        servidores.darDeBaja(servidorId, detalle.organizacionId());

        // Sin esto, el plugin de un servidor archivado seguiria autenticandose
        // para siempre: el dueño lo dio de baja en la web pero el JAR sigue
        // instalado en su maquina, latiendo cada minuto.
        servidores.revocarClaves(servidorId);

        log.info("Servidor {} dado de baja por {}", servidorId, usuario.id());
        return java.util.Map.of("dadoDeBaja", true);
    }

    /** Vacio se guarda como null: una cadena vacia en base de datos no significa nada. */
    private String recortar(String valor, int max) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.length() > max ? limpio.substring(0, max) : limpio;
    }

    /** Organizaciones del usuario, con su plan y cuantos servidores le quedan. */
    @GetMapping("/mis-organizaciones")
    public List<OrganizacionPropia> misOrganizaciones(@UsuarioActual IdentityRepository.Usuario usuario) {
        return organizaciones.organizacionesDe(usuario.id()).stream()
                .map(org -> {
                    Integer limite = organizaciones.limiteDeServidores(org.id());
                    int usados = servidores.contarDe(org.id());
                    return new OrganizacionPropia(
                            org.id(), org.nombre(), org.plan(), org.rol(), usados, limite);
                })
                .toList();
    }

    /**
     * Resuelve sobre que organizacion se opera.
     *
     * <p>Sin indicar ninguna se usa la unica del usuario. Si tiene varias hay que
     * decir cual, y en cualquier caso se verifica que puede gestionarla.
     */
    private UUID organizacionDe(IdentityRepository.Usuario usuario, UUID solicitada) {
        var membresias = organizaciones.organizacionesDe(usuario.id());

        if (solicitada == null) {
            if (membresias.size() == 1) {
                return membresias.get(0).id();
            }
            throw new DomainException.Conflict(
                    "organization.required", "Indica en que organizacion quieres crear el servidor.");
        }

        if (!organizaciones.puedeGestionar(usuario.id(), solicitada)) {
            // Mismo error que "no existe": confirmar que la organizacion existe
            // pero no es tuya ya seria filtrar informacion.
            throw new DomainException.NotFound(
                    "organization.not_found", "Esa organizacion no existe o no puedes gestionarla.");
        }
        return solicitada;
    }

    /**
     * @param organizacionId opcional; solo hace falta si el usuario pertenece a
     *     varias organizaciones
     */
    public record PeticionRegistro(
            @NotBlank @Size(max = 64) String slug,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 500) String descripcion,
            @NotBlank @Size(max = 255) String host,
            @Min(1) @Max(65535) int puerto,
            boolean aceptaOffline,
            UUID organizacionId) {}

    public record RespuestaRegistro(UUID servidorId, String slug, String claveApi, String aviso) {}

    /**
     * @param iconoUrl null si aún no ha subido ninguno
     * @param verificado true en cuanto el plugin se conecta por primera vez
     */
    public record FichaServidor(
            UUID id,
            String slug,
            String nombre,
            String descripcion,
            String estado,
            String host,
            int puerto,
            boolean aceptaOffline,
            boolean verificado,
            String iconoUrl,
            String bannerUrl,
            String versionMinima,
            String versionMaxima,
            String region,
            String tema,
            List<String> modalidades) {}

    /**
     * @param modalidades codigos del catalogo; los que no existan los rechaza la
     *     clave ajena, que es donde deben rechazarse
     */
    /** @param nombre tiene que coincidir con el del servidor; se valida en el backend */
    public record PeticionBaja(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80)
                    String nombre) {}

    public record PeticionFicha(
            @jakarta.validation.constraints.Size(max = 500) String descripcion,
            @jakarta.validation.constraints.Size(max = 12) String versionMinima,
            @jakarta.validation.constraints.Size(max = 12) String versionMaxima,
            @jakarta.validation.constraints.Size(max = 40) String region,
            @jakarta.validation.constraints.Size(max = 20) String tema,
            @jakarta.validation.constraints.Size(max = 8) List<String> modalidades) {}

    public record ServidorPropio(
            UUID id,
            String slug,
            String nombre,
            String descripcion,
            String estado,
            String organizacion,
            String plan) {}

    /**
     * @param limiteServidores null = sin limite (plan Plus)
     */
    public record OrganizacionPropia(
            UUID id, String nombre, String plan, String rol, int servidoresUsados, Integer limiteServidores) {}
}
