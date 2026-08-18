package com.latinosmc.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.shared.DomainException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Compra de espacios destacados y su rastro contable.
 *
 * <p>Lo que se prueba aqui no es que las fechas salgan bien: es que el dinero no
 * se cuente dos veces, que nada se reserve sin haberse cobrado y que un cobro
 * bueno nunca se pierda por un tropiezo posterior. Son los tres fallos que en
 * pagos se descubren tarde y salen caros.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompraDeSlotServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-14T10:00:00Z");
    private static final UUID SERVIDOR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORGANIZACION = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private BillingRepository repositorio;

    private CompraDeSlotService servicio;

    private final BillingRepository.Slot slot1 = new BillingRepository.Slot(1, 2500, "EUR", 7, 4, true);

    @BeforeEach
    void preparar() {
        servicio = new CompraDeSlotService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

        when(repositorio.slots()).thenReturn(List.of(slot1));
        when(repositorio.slot(1)).thenReturn(Optional.of(slot1));
        when(repositorio.buscarPorClaveIdempotencia(anyString())).thenReturn(Optional.empty());
        when(repositorio.finDeLaCola(anyInt(), any())).thenReturn(Optional.empty());
        when(repositorio.periodosEnCola(anyInt(), any())).thenReturn(0);
        when(repositorio.crearPedido(any(), any(), any(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    // --- Iniciar la compra ---------------------------------------------------

    @Nested
    @DisplayName("Al iniciar la compra")
    class Iniciar {

        /**
         * El fallo que esto impide es el mas caro del modulo: si reservar no
         * exigiera pagar, cualquiera bloquea los cinco espacios gratis abriendo
         * compras que no termina.
         */
        @Test
        @DisplayName("el pedido nace pendiente y no reserva nada todavia")
        void noReservaAntesDeCobrar() {
            var reserva = servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-1");

            assertThat(reserva.yaExistia()).isFalse();
            verify(repositorio, never()).crearReserva(any(), anyInt(), any(), any(), any(), any());
            verify(repositorio, never()).registrarAsiento(any(), any(), any(), anyString(), anyLong(), anyString(), anyString(), any());
        }

        /**
         * El precio se lee del catalogo, nunca de la peticion. Aceptarlo del
         * cliente es el error clasico de las integraciones de pago: comprar un
         * espacio de 25 € por un centimo.
         */
        @Test
        @DisplayName("el importe del pedido sale del catalogo, no de quien compra")
        void elPrecioLoPoneElCatalogo() {
            var reserva = servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-precio");

            assertThat(reserva.importeCentimos()).isEqualTo(2500);
            assertThat(reserva.moneda()).isEqualTo("EUR");
            verify(repositorio)
                    .crearPedido(any(), eq(ORGANIZACION), eq(USUARIO), eq("FEATURED_SLOT"), eq(2500L), eq("EUR"), eq("clave-precio"), anyString());
        }

        @Test
        @DisplayName("un doble clic devuelve el mismo pedido en vez de crear otro")
        void idempotencia() {
            var pedido = pedido("PENDING");
            when(repositorio.buscarPorClaveIdempotencia("clave-repetida")).thenReturn(Optional.of(pedido));

            var reserva = servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-repetida");

            assertThat(reserva.yaExistia()).isTrue();
            assertThat(reserva.pedidoId()).isEqualTo(pedido.id());
            verify(repositorio, never())
                    .crearPedido(any(), any(), any(), anyString(), anyLong(), anyString(), anyString(), anyString());
        }

        /**
         * Dos peticiones a la vez con la misma clave: ambas pasan la
         * comprobacion previa y solo una gana contra el indice unico. La
         * perdedora tiene que devolver el pedido de la ganadora, no reventar.
         */
        @Test
        @DisplayName("si pierde la carrera de idempotencia, devuelve el pedido ganador")
        void carreraDeIdempotencia() {
            var ganador = pedido("PENDING");
            when(repositorio.crearPedido(any(), any(), any(), anyString(), anyLong(), anyString(), eq("clave-carrera"), anyString()))
                    .thenReturn(false);
            when(repositorio.buscarPorClaveIdempotencia("clave-carrera"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(ganador));

            var reserva = servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-carrera");

            assertThat(reserva.yaExistia()).isTrue();
            assertThat(reserva.pedidoId()).isEqualTo(ganador.id());
        }

        @Test
        @DisplayName("un espacio desactivado no se puede comprar")
        void slotDesactivado() {
            when(repositorio.slot(1)).thenReturn(Optional.of(new BillingRepository.Slot(1, 2500, "EUR", 7, 4, false)));

            assertThatThrownBy(() -> servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-off"))
                    .isInstanceOf(DomainException.NotFound.class)
                    .hasFieldOrPropertyWithValue("code", "slot.not_available");
        }

        /**
         * El tope de cola existe para no vender un espacio con seis meses de
         * antelacion: el comprador se olvidaria de que lo compro y nosotros
         * tendriamos el dinero de un servicio que aun no se ha prestado.
         */
        @Test
        @DisplayName("con la cola llena no se admite otra compra")
        void colaLlena() {
            when(repositorio.periodosEnCola(1, AHORA)).thenReturn(4);

            assertThatThrownBy(() -> servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-llena"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasFieldOrPropertyWithValue("code", "slot.queue_full");
        }

        @Test
        @DisplayName("con el espacio libre, el periodo empieza ahora y dura lo que dice el catalogo")
        void periodoInmediato() {
            var reserva = servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-libre");

            assertThat(reserva.desde()).isEqualTo(AHORA);
            assertThat(reserva.hasta()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
        }

        /**
         * Nunca un "proximamente" sin compromiso: quien compra un espacio
         * ocupado tiene que saber el dia exacto en que empieza el suyo.
         */
        @Test
        @DisplayName("con el espacio ocupado, el periodo empieza cuando acaba la cola")
        void periodoEnCola() {
            Instant finDeLaCola = AHORA.plus(Duration.ofDays(3));
            when(repositorio.finDeLaCola(1, AHORA)).thenReturn(Optional.of(finDeLaCola));

            var reserva = servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-cola");

            assertThat(reserva.desde()).isEqualTo(finDeLaCola);
            assertThat(reserva.hasta()).isEqualTo(finDeLaCola.plus(Duration.ofDays(7)));
        }

        /**
         * Lo que se compro lo decidimos nosotros al crear el pedido, y viaja en
         * los metadatos. La pasarela solo confirma que se cobro.
         */
        @Test
        @DisplayName("las fechas y el servidor quedan escritos en el pedido, no llegan luego del webhook")
        void metadatosEscritosPorNosotros() {
            servicio.iniciarCompra(1, SERVIDOR, ORGANIZACION, USUARIO, "clave-meta");

            var metadatos = ArgumentCaptor.forClass(String.class);
            verify(repositorio)
                    .crearPedido(any(), any(), any(), anyString(), anyLong(), anyString(), anyString(), metadatos.capture());

            assertThat(metadatos.getValue())
                    .contains("\"tipo\":\"FEATURED_SLOT\"")
                    .contains("\"slot\":1")
                    .contains(SERVIDOR.toString())
                    .contains(AHORA.toString());
        }
    }

    // --- Confirmar el pago ---------------------------------------------------

    @Nested
    @DisplayName("Al confirmar el pago")
    class Confirmar {

        @Test
        @DisplayName("se anota el asiento y se materializa la reserva")
        void cobroCompleto() {
            var pedido = pedido("PENDING");
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.marcarPagado(eq(pedido.id()), anyString(), anyString(), any())).thenReturn(1);
            when(repositorio.metadatosDePedido(pedido.id()))
                    .thenReturn(Optional.of(new BillingRepository.MetadatosDeSlot(
                            1, SERVIDOR, AHORA, AHORA.plus(Duration.ofDays(7)))));

            servicio.confirmarPago(pedido.id(), "paypal", "REF-1");

            verify(repositorio)
                    .registrarAsiento(any(), eq(ORGANIZACION), eq(pedido.id()), eq("PAYMENT_RECEIVED"), eq(2500L), eq("EUR"), anyString(), eq(null));
            verify(repositorio)
                    .crearReserva(any(), eq(1), eq(SERVIDOR), eq(pedido.id()), eq(AHORA), eq(AHORA.plus(Duration.ofDays(7))));
        }

        /**
         * El reenvio del webhook es lo normal, no la excepcion: las pasarelas
         * insisten hasta recibir un 200. Si contase dos veces, los ingresos del
         * panel dejarian de cuadrar con el banco.
         */
        @Test
        @DisplayName("un webhook repetido no vuelve a anotar el cobro")
        void webhookDuplicado() {
            var pedido = pedido("PAID");
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.marcarPagado(eq(pedido.id()), anyString(), anyString(), any())).thenReturn(0);

            servicio.confirmarPago(pedido.id(), "paypal", "REF-1");

            verify(repositorio, never())
                    .registrarAsiento(any(), any(), any(), anyString(), anyLong(), anyString(), anyString(), any());
            verify(repositorio, never()).crearReserva(any(), anyInt(), any(), any(), any(), any());
        }

        /**
         * El importe del asiento sale del pedido que guardamos nosotros. Si
         * saliera del mensaje de la pasarela, quien lograse falsificar uno
         * escribiria en nuestra contabilidad.
         */
        @Test
        @DisplayName("el asiento usa el importe del pedido, no el del mensaje recibido")
        void elImporteSaleDelPedido() {
            var pedido = pedido("PENDING");
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.marcarPagado(eq(pedido.id()), anyString(), anyString(), any())).thenReturn(1);
            when(repositorio.metadatosDePedido(pedido.id())).thenReturn(Optional.empty());

            servicio.confirmarPago(pedido.id(), "paypal", "REF-1");

            var importe = ArgumentCaptor.forClass(Long.class);
            verify(repositorio)
                    .registrarAsiento(any(), any(), any(), eq("PAYMENT_RECEIVED"), importe.capture(), anyString(), anyString(), any());
            assertThat(importe.getValue()).isEqualTo(2500L);
        }

        @Test
        @DisplayName("un producto que no es un espacio se cobra pero no reserva nada")
        void otroProducto() {
            var pedido = new BillingRepository.Pedido(
                    UUID.randomUUID(), ORGANIZACION, "PLAN_PLUS", "PENDING", 900, "EUR", null, null);
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.marcarPagado(eq(pedido.id()), anyString(), anyString(), any())).thenReturn(1);

            servicio.confirmarPago(pedido.id(), "paypal", "REF-2");

            verify(repositorio)
                    .registrarAsiento(any(), any(), eq(pedido.id()), eq("PAYMENT_RECEIVED"), eq(900L), eq("EUR"), anyString(), any());
            verify(repositorio, never()).crearReserva(any(), anyInt(), any(), any(), any(), any());
        }

        /**
         * Si dos compras del mismo hueco se pagan a la vez, la base de datos
         * impide el solape. El cobro sigue siendo valido: perder el dinero del
         * cliente seria peor que reasignar el espacio a mano.
         */
        @Test
        @DisplayName("un solape al reservar no tumba el cobro ya anotado")
        void solapeAlReservar() {
            var pedido = pedido("PENDING");
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.marcarPagado(eq(pedido.id()), anyString(), anyString(), any())).thenReturn(1);
            when(repositorio.metadatosDePedido(pedido.id()))
                    .thenReturn(Optional.of(new BillingRepository.MetadatosDeSlot(
                            1, SERVIDOR, AHORA, AHORA.plus(Duration.ofDays(7)))));
            org.mockito.Mockito.doThrow(new DataIntegrityViolationException("solape"))
                    .when(repositorio)
                    .crearReserva(any(), anyInt(), any(), any(), any(), any());

            assertThatCode(() -> servicio.confirmarPago(pedido.id(), "paypal", "REF-3"))
                    .doesNotThrowAnyException();

            verify(repositorio)
                    .registrarAsiento(any(), any(), any(), eq("PAYMENT_RECEIVED"), anyLong(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("un pedido pagado sin metadatos no revienta: queda para revision")
        void sinMetadatos() {
            var pedido = pedido("PENDING");
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.marcarPagado(eq(pedido.id()), anyString(), anyString(), any())).thenReturn(1);
            when(repositorio.metadatosDePedido(pedido.id())).thenReturn(Optional.empty());

            assertThatCode(() -> servicio.confirmarPago(pedido.id(), "paypal", "REF-4"))
                    .doesNotThrowAnyException();

            verify(repositorio, never()).crearReserva(any(), anyInt(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("un pedido desconocido se rechaza")
        void pedidoDesconocido() {
            UUID id = UUID.randomUUID();
            when(repositorio.buscarPedido(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.confirmarPago(id, "paypal", "REF-5"))
                    .isInstanceOf(DomainException.NotFound.class)
                    .hasFieldOrPropertyWithValue("code", "order.not_found");
        }
    }

    // --- Reembolsos ----------------------------------------------------------

    @Nested
    @DisplayName("Al reembolsar")
    class Reembolsar {

        /**
         * El ledger no se corrige: se le añade. Un UPDATE sobre un asiento
         * borraria la explicacion de como se llego al saldo, que es exactamente
         * para lo que sirve un libro mayor.
         */
        @Test
        @DisplayName("se añade un asiento de signo contrario, no se toca el original")
        void asientoCompensatorio() {
            var pedido = pedido("PAID");
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.existeAsiento(pedido.id(), "REFUND")).thenReturn(false);

            servicio.registrarReembolso(pedido.id(), "el cliente se arrepintio");

            verify(repositorio)
                    .registrarAsiento(any(), eq(ORGANIZACION), eq(pedido.id()), eq("REFUND"), eq(-2500L), eq("EUR"), anyString(), eq(null));
        }

        /**
         * Sin esto, un reenvio del evento de reembolso devolveria el dinero dos
         * veces en el libro y la organizacion apareceria debiendo dinero que
         * nunca cobro.
         */
        @Test
        @DisplayName("un evento de reembolso repetido no vuelve a restar")
        void reembolsoDuplicado() {
            var pedido = pedido("REFUNDED");
            when(repositorio.buscarPedido(pedido.id())).thenReturn(Optional.of(pedido));
            when(repositorio.existeAsiento(pedido.id(), "REFUND")).thenReturn(true);

            servicio.registrarReembolso(pedido.id(), "reenvio de la pasarela");

            verify(repositorio, never())
                    .registrarAsiento(any(), any(), any(), anyString(), anyLong(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("un pedido desconocido se rechaza")
        void pedidoDesconocido() {
            UUID id = UUID.randomUUID();
            when(repositorio.buscarPedido(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrarReembolso(id, "motivo"))
                    .isInstanceOf(DomainException.NotFound.class)
                    .hasFieldOrPropertyWithValue("code", "order.not_found");
        }
    }

    // --- Escaparate ----------------------------------------------------------

    @Nested
    @DisplayName("La disponibilidad que ve quien compra")
    class Disponibilidad {

        @Test
        @DisplayName("un espacio libre se ofrece con sus fechas exactas")
        void libre() {
            var vista = servicio.disponibilidad();

            assertThat(vista).singleElement().satisfies(d -> {
                assertThat(d.disponible()).isTrue();
                assertThat(d.proximoDesde()).isEqualTo(AHORA);
                assertThat(d.proximoHasta()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
                assertThat(d.periodosEnCola()).isZero();
            });
        }

        @Test
        @DisplayName("con la cola llena se muestra, pero no se puede comprar")
        void colaLlena() {
            when(repositorio.periodosEnCola(1, AHORA)).thenReturn(4);

            assertThat(servicio.disponibilidad()).singleElement().satisfies(d -> {
                assertThat(d.disponible()).isFalse();
                assertThat(d.periodosEnCola()).isEqualTo(4);
            });
        }

        @Test
        @DisplayName("un espacio desactivado nunca aparece como disponible")
        void desactivado() {
            when(repositorio.slots()).thenReturn(List.of(new BillingRepository.Slot(1, 2500, "EUR", 7, 4, false)));

            assertThat(servicio.disponibilidad()).singleElement().satisfies(d -> assertThat(d.disponible())
                    .isFalse());
        }
    }

    private static BillingRepository.Pedido pedido(String estado) {
        return new BillingRepository.Pedido(
                UUID.randomUUID(), ORGANIZACION, "FEATURED_SLOT", estado, 2500, "EUR", null, null);
    }
}
