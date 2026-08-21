package com.latinosmc.app.mantenimiento;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.ports.FileStorage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La limpieza que el ADR-0017 daba por hecha y nunca existio.
 *
 * <p>El caso que importa no es el feliz: es el del almacen caido. Marcar la fila
 * antes de borrar de verdad significaria que un corte de R2 deja archivos
 * pagados para siempre y ademas invisibles, porque nadie volveria a mirarlos.
 */
class LimpiezaDeArchivosHuerfanosTest {

    private ServerRepository servidores;
    private FileStorage almacen;
    private LimpiezaDeArchivosHuerfanos limpieza;

    @BeforeEach
    void preparar() {
        servidores = mock(ServerRepository.class);
        almacen = mock(FileStorage.class);
        limpieza = new LimpiezaDeArchivosHuerfanos(servidores, almacen, 7);
    }

    @Test
    @DisplayName("borra el archivo y despues marca la fila")
    void borraYMarca() {
        when(servidores.huerfanosParaBorrar(anyInt(), anyInt())).thenReturn(List.of("a/uno.png"));

        limpieza.ronda();

        verify(almacen).borrar("a/uno.png");
        verify(servidores).marcarHuerfanoBorrado("a/uno.png");
    }

    @Test
    @DisplayName("si el almacen falla, la fila NO se marca: se reintenta mañana")
    void almacenCaido() {
        when(servidores.huerfanosParaBorrar(anyInt(), anyInt())).thenReturn(List.of("a/uno.png"));
        doThrow(new IllegalStateException("R2 caido")).when(almacen).borrar("a/uno.png");

        limpieza.ronda();

        // Marcarla seria perder la referencia: el archivo seguiria ahi,
        // pagandose, y ya nadie sabria que existe.
        verify(servidores, never()).marcarHuerfanoBorrado(anyString());
    }

    @Test
    @DisplayName("un archivo que falla no impide borrar los demas")
    void unFalloNoCortaLaRonda() {
        when(servidores.huerfanosParaBorrar(anyInt(), anyInt()))
                .thenReturn(List.of("a/uno.png", "a/dos.png", "a/tres.png"));
        doThrow(new IllegalStateException("permiso denegado")).when(almacen).borrar("a/dos.png");

        limpieza.ronda();

        verify(servidores).marcarHuerfanoBorrado("a/uno.png");
        verify(servidores).marcarHuerfanoBorrado("a/tres.png");
        verify(servidores, never()).marcarHuerfanoBorrado("a/dos.png");
    }

    @Test
    @DisplayName("sin huerfanos no llama al almacen")
    void sinNada() {
        when(servidores.huerfanosParaBorrar(anyInt(), anyInt())).thenReturn(List.of());

        limpieza.ronda();

        verify(almacen, never()).borrar(anyString());
    }
}
