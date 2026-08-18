package com.latinosmc.plugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code /votar} command.
 *
 * <p>One command only. After handing out the code it schedules a single reward
 * check, so the player never has to run the command a second time to claim.
 */
public final class ComandoVotar implements CommandExecutor {

    /**
     * Espera minima entre dos {@code /votar} del mismo jugador.
     *
     * <p>No es por comodidad: cada ejecucion pide un codigo a la plataforma, y la
     * plataforma limita el trafico <em>por servidor</em>. Un jugador aporreando
     * el comando consumiria el cupo de todos los demas, y el sintoma seria que
     * "el plugin no funciona" para el servidor entero.
     */
    private static final long ESPERA_ENTRE_INTENTOS_MS = 3_000;

    private final LatinosMcPlugin plugin;
    private final ClienteApi api;
    private final Mensajes mensajes;
    private final boolean modoOffline;

    private final Map<UUID, Long> ultimoIntento = new ConcurrentHashMap<>();

    public ComandoVotar(LatinosMcPlugin plugin, ClienteApi api, Mensajes mensajes, boolean modoOffline) {
        this.plugin = plugin;
        this.api = api;
        this.mensajes = mensajes;
        this.modoOffline = modoOffline;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender emisor, @NotNull Command comando, @NotNull String alias, @NotNull String[] args) {
        if (!(emisor instanceof Player jugador)) {
            emisor.sendMessage("Este comando solo funciona dentro del juego.");
            return true;
        }

        if (demasiadoSeguido(jugador)) {
            jugador.sendMessage(mensajes.linea("espera", "<gray>Un momento, que ya estoy en ello...</gray>"));
            return true;
        }

        jugador.sendMessage(mensajes.linea("preparando", "<gray>Preparando tu voto...</gray>"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String tipo = modoOffline ? "OFFLINE_NAME" : "MOJANG_UUID";
                String identidad = modoOffline ? jugador.getName() : jugador.getUniqueId().toString();

                ClienteApi.Voto voto = api.pedirVoto(tipo, identidad);

                if (!voto.puedeVotar()) {
                    avisarQueYaVoto(jugador, voto);
                    return;
                }

                entregarCodigo(jugador, voto);
                programarComprobacion(jugador);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "No se pudo preparar el voto", e);
                jugador.sendMessage(mensajes.linea(
                        "error",
                        "<red>No se ha podido preparar tu voto ahora mismo. Inténtalo en un momento.</red>"));
            }
        });

        return true;
    }

    private boolean demasiadoSeguido(Player jugador) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimoIntento.put(jugador.getUniqueId(), ahora);
        return anterior != null && ahora - anterior < ESPERA_ENTRE_INTENTOS_MS;
    }

    private void avisarQueYaVoto(Player jugador, ClienteApi.Voto voto) {
        String restante = Mensajes.enPalabras(Duration.ofSeconds(voto.segundosRestantes()));

        String clave = "YA_VOTO_HOY".equals(voto.motivo()) ? "ya-voto-hoy" : "ya-voto-aqui";
        String pordefecto = "YA_VOTO_HOY".equals(voto.motivo())
                ? "<yellow>Ya has votado hoy.</yellow> <gray>Podrás volver a votar en {restante}.</gray>"
                : "<yellow>Ya votaste a este servidor.</yellow> <gray>Vuelve en {restante}.</gray>";

        jugador.sendMessage(mensajes.linea(clave, pordefecto, "restante", restante));
    }

    private void entregarCodigo(Player jugador, ClienteApi.Voto voto) {
        int espera = plugin.getConfig().getInt("voto.espera-segundos", 60);

        for (Component linea : mensajes.bloque(
                "voto",
                List.of(
                        "",
                        "<green>¡Vota y llévate tu recompensa!</green>",
                        "<gray>Si puedes pulsar enlaces:</gray>",
                        "",
                        "<gray>O entra en <white>{sitio}</white> y escribe este código:</gray>",
                        "<aqua><bold>{codigo}</bold></aqua>",
                        "",
                        "<dark_gray>Tu recompensa llegará sola unos {espera} segundos después de votar.</dark_gray>",
                        "<dark_gray>No hace falta que escribas nada más. El código caduca en 10 minutos.</dark_gray>",
                        ""),
                "codigo", voto.codigo(),
                "sitio", sitioDe(voto.url()),
                "espera", espera)) {
            jugador.sendMessage(linea);
        }

        jugador.sendMessage(mensajes
                .linea("boton", "  <aqua><bold>▶ PULSA AQUÍ PARA VOTAR</bold></aqua>")
                .clickEvent(ClickEvent.openUrl(voto.url()))
                .hoverEvent(HoverEvent.showText(
                        mensajes.linea("boton-encima", "<gray>Abre el enlace en tu navegador</gray>"))));
    }

    private void programarComprobacion(Player jugador) {
        int espera = plugin.getConfig().getInt("voto.espera-segundos", 60);
        long ticks = Math.max(20L, espera * 20L);

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!jugador.isOnline()) {
                return;
            }
            plugin.recogerRecompensas();
        }, ticks);
    }

    private String sitioDe(String url) {
        try {
            return java.net.URI.create(url).getHost();
        } catch (RuntimeException e) {
            return url;
        }
    }
}
