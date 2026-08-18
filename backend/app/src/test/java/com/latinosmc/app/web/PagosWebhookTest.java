package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.billing.CompraDeSlotService;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.ports.PaymentGateway;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Que le pasa a un cobro cuando algo se tuerce.
 *
 * <p>La parte dificil de una integracion de pagos no es el camino bueno: es que
 * ningun evento de dinero se pierda cuando el camino bueno falla. La pasarela
 * reenvia hasta recibir un 200, asi que la regla es que solo se le diga "ya esta"
 * cuando de verdad lo esta (ADR-0021).
 *
 * <p>Sin estos tests el fallo aparece meses despues, en forma de un cliente que
 * pago y nunca salio destacado, sin ninguna traza que lo explique.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagosWebhookTest {

    private static final String PASARELA = "paypal";
    private static final UUID PEDIDO = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private CompraDeSlotService compras;

    @Mock
    private BillingRepository facturacion;

    @Mock
    private PaymentGateway pasarela;

    private PagosController controlador;

    private final HttpServletRequest peticion = mock(HttpServletRequest.class);

    @BeforeEach
    void preparar() {
        controlador = new PagosController(
                compras,
                facturacion,
                mock(ServerRepository.class),
                mock(OrganizationRepository.class),
                pasarela,
                "https://latinosmc.com");

        when(pasarela.codigo()).thenReturn(PASARELA);
        when(facturacion.registrarWebhook(any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    /**
     * La firma es lo unico que distingue un cobro real de un mensaje que se ha
     * inventado quien descubrio la URL. Sin ella no se toca nada, ni siquiera se
     * registra: apuntar lo que llega sin verificar convierte la tabla de eventos
     * en un buzon abierto.
     */
    @Test
    @DisplayName("un webhook sin firma valida se descarta sin tocar nada")
    void firmaInvalida() {
        when(pasarela.verificarWebhook(anyString(), any())).thenReturn(Optional.empty());

        var respuesta = controlador.webhook("{}", peticion);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        verify(facturacion, never()).registrarWebhook(any(), anyString(), anyString(), anyString(), anyString());
        verify(compras, never()).confirmarPago(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("un cobro valido se aplica y el evento se cierra")
    void cobroAplicado() {
        firmaValida("payment.succeeded");

        var respuesta = controlador.webhook("{}", peticion);

        assertThat(respuesta.getStatusCode().is2xxSuccessful()).isTrue();
        verify(compras).confirmarPago(PEDIDO, PASARELA, "REF-1");
        verify(facturacion).marcarWebhookProcesado(PASARELA, "evt-1", null);
    }

    @Test
    @DisplayName("un reembolso valido se aplica y el evento se cierra")
    void reembolsoAplicado() {
        firmaValida("payment.refunded");

        controlador.webhook("{}", peticion);

        verify(compras).registrarReembolso(eq(PEDIDO), anyString());
        verify(facturacion).marcarWebhookProcesado(PASARELA, "evt-1", null);
    }

    @Test
    @DisplayName("un evento ya terminado no se vuelve a aplicar")
    void eventoYaProcesado() {
        firmaValida("payment.succeeded");
        when(facturacion.registrarWebhook(any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        var respuesta = controlador.webhook("{}", peticion);

        assertThat(respuesta.getStatusCode().is2xxSuccessful()).isTrue();
        verify(compras, never()).confirmarPago(any(), anyString(), anyString());
    }

    /**
     * El fallo que este test impide: si un tropiezo pasajero cerrase el evento,
     * el reenvio de la pasarela lo descartaria por conocido y el cobro se
     * quedaria sin asiento y sin espacio reservado. En silencio, ademas: la
     * pasarela habria recibido su 200 y nadie volveria a mirar.
     */
    @Test
    @DisplayName("un fallo pasajero deja el evento pendiente y pide reintento")
    void falloPasajeroNoCierraElEvento() {
        firmaValida("payment.succeeded");
        doThrow(new DataAccessResourceFailureException("la base de datos no responde"))
                .when(compras)
                .confirmarPago(any(), anyString(), anyString());

        var respuesta = controlador.webhook("{}", peticion);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
        verify(facturacion).anotarFalloDeWebhook(eq(PASARELA), eq("evt-1"), anyString());
        verify(facturacion, never()).marcarWebhookProcesado(anyString(), anyString(), any());
    }

    /**
     * La otra mitad de lo mismo: que el evento siga pendiente no sirve de nada si
     * el reintento no llega a aplicarse. Aqui se comprueba la secuencia completa
     * —falla, la pasarela reenvia, se aplica— que es como acaba ocurriendo en
     * produccion.
     */
    @Test
    @DisplayName("el reenvio de un evento que fallo si se aplica")
    void elReintentoSiSeAplica() {
        firmaValida("payment.succeeded");
        doThrow(new DataAccessResourceFailureException("caida momentanea"))
                .doNothing()
                .when(compras)
                .confirmarPago(any(), anyString(), anyString());

        assertThat(controlador.webhook("{}", peticion).getStatusCode().value()).isEqualTo(500);
        var reintento = controlador.webhook("{}", peticion);

        assertThat(reintento.getStatusCode().is2xxSuccessful()).isTrue();
        verify(compras, org.mockito.Mockito.times(2)).confirmarPago(PEDIDO, PASARELA, "REF-1");
        verify(facturacion).marcarWebhookProcesado(PASARELA, "evt-1", null);
    }

    /**
     * Un pedido que no existe no va a aparecer por insistir. Se cierra con el
     * motivo escrito —queda para conciliar, porque un cobro sin pedido nuestro
     * merece una mirada— y se responde 200 para que la pasarela pare.
     */
    @Test
    @DisplayName("un fallo definitivo se cierra con su motivo y no se reintenta")
    void falloDefinitivo() {
        firmaValida("payment.succeeded");
        doThrow(new DomainException.NotFound("order.not_found", "Pedido desconocido."))
                .when(compras)
                .confirmarPago(any(), anyString(), anyString());

        var respuesta = controlador.webhook("{}", peticion);

        assertThat(respuesta.getStatusCode().is2xxSuccessful()).isTrue();
        verify(facturacion).marcarWebhookProcesado(eq(PASARELA), eq("evt-1"), anyString());
        verify(facturacion, never()).anotarFalloDeWebhook(anyString(), anyString(), anyString());
    }

    /**
     * El cuerpo se guarda tal cual llego. Es lo unico que permite reconstruir
     * meses despues que dijo exactamente la pasarela.
     */
    @Test
    @DisplayName("el evento se registra con el cuerpo exacto que llego")
    void seGuardaElCuerpoCrudo() {
        firmaValida("payment.succeeded");
        String cuerpo = "{\"id\":\"evt-1\",\"tipo\":\"cobro\"}";

        controlador.webhook(cuerpo, peticion);

        verify(facturacion).registrarWebhook(any(), eq(PASARELA), eq("evt-1"), eq("payment.succeeded"), eq(cuerpo));
    }

    private void firmaValida(String tipo) {
        when(pasarela.verificarWebhook(anyString(), any()))
                .thenReturn(Optional.of(
                        new PaymentGateway.EventoDePago("evt-1", tipo, PEDIDO, "REF-1", 2500, "EUR")));
    }
}
