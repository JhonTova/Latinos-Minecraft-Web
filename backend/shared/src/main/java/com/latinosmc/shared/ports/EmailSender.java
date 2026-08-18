package com.latinosmc.shared.ports;

/**
 * Envio de correo. Puerto, no implementacion.
 *
 * <p>Se declara como interfaz desde el primer dia para no acoplarse al proveedor.
 * Los servicios de correo cambian de precio, de politica y de reputacion de
 * entrega con frecuencia; migrar debe ser escribir otro adaptador, no tocar el
 * dominio.
 */
public interface EmailSender {

    /**
     * @param destinatario direccion de correo
     * @param asunto titulo del mensaje
     * @param cuerpoTexto version en texto plano. Obligatoria: hay clientes que no
     *     muestran HTML, y un correo solo-HTML puntua peor en los filtros de spam.
     * @param cuerpoHtml version enriquecida; puede ser null
     */
    void enviar(String destinatario, String asunto, String cuerpoTexto, String cuerpoHtml);
}
