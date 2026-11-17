package com.latinosmc.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.identity.VerificationRepository.CodigoVigente;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.ports.EmailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * CA-3: el intento fallido que se suma tiene que persistir aunque la peticion
 * termine en error.
 *
 * <p>Una prueba solo con Mockito no ve este fallo: sin el proxy transaccional de
 * Spring, la excepcion no revierte nada. Aqui el servicio real queda envuelto en
 * su proxy y el gestor de transacciones es un doble, asi que se puede preguntar
 * si la transaccion termino en {@code commit} o en {@code rollback}.
 *
 * <p>Limite conocido: no demuestra que el UPDATE llegue a PostgreSQL (ADR-0021).
 * Demuestra la semantica del proxy, que es donde esta el fallo.
 */
class EmailVerificationServiceTransaccionTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T10:00:00Z");
    private static final UUID USUARIO = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID CODIGO_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private AnnotationConfigApplicationContext contexto;
    private PlatformTransactionManager gestor;
    private VerificationRepository repositorio;
    private EmailVerificationService servicio;

    @Configuration
    @EnableTransactionManagement
    static class Configuracion {

        @Bean
        PlatformTransactionManager transactionManager() {
            PlatformTransactionManager gestor = mock(PlatformTransactionManager.class);
            when(gestor.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            return gestor;
        }

        @Bean
        VerificationRepository verificationRepository() {
            return mock(VerificationRepository.class);
        }

        @Bean
        EmailSender emailSender() {
            return mock(EmailSender.class);
        }

        @Bean
        Clock reloj() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }

        @Bean
        EmailVerificationService emailVerificationService(
                VerificationRepository repositorio, EmailSender correo, Clock reloj) {
            return new EmailVerificationService(repositorio, correo, reloj);
        }
    }

    @BeforeEach
    void preparar() {
        contexto = new AnnotationConfigApplicationContext(Configuracion.class);
        gestor = contexto.getBean(PlatformTransactionManager.class);
        repositorio = contexto.getBean(VerificationRepository.class);
        servicio = contexto.getBean(EmailVerificationService.class);
        // Se descarta la llamada de la preparacion para contar solo las del servicio.
        reset(gestor);
        when(gestor.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
    }

    @AfterEach
    void cerrar() {
        contexto.close();
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("el servicio esta envuelto en el proxy transaccional")
    void elServicioTieneProxyTransaccional() {
        assertThat(org.springframework.aop.support.AopUtils.isAopProxy(servicio)).isTrue();
    }

    @Test
    @DisplayName("un codigo incorrecto suma el intento y la transaccion hace commit, no rollback")
    void codigoIncorrectoHaceCommitYNoRollback() {
        when(repositorio.codigoVigente(USUARIO, AHORA))
                .thenReturn(Optional.of(new CodigoVigente(CODIGO_ID, sha256("123456"), "alex@ejemplo.com", 0)));

        assertThatThrownBy(() -> servicio.verificar(USUARIO, "999999"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).code())
                .isEqualTo("verification.invalid_code");

        verify(repositorio).sumarIntento(CODIGO_ID);
        verify(gestor).commit(any(TransactionStatus.class));
        verify(gestor, never()).rollback(any(TransactionStatus.class));
    }

    @Test
    @DisplayName("control: un codigo caducado sigue haciendo rollback")
    void caducadoHaceRollback() {
        when(repositorio.codigoVigente(USUARIO, AHORA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.verificar(USUARIO, "123456"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).code())
                .isEqualTo("verification.expired");

        verify(gestor).rollback(any(TransactionStatus.class));
        verify(gestor, never()).commit(any(TransactionStatus.class));
    }
}
