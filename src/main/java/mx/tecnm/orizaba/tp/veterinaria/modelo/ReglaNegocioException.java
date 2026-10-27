package mx.tecnm.orizaba.tp.veterinaria.modelo;

import java.io.Serial;

/**
 * Excepción no comprobada que señala la violación de una invariante del dominio.
 * <p>Encapsulamiento (Booch): los objetos protegen su estado rechazando valores inválidos.</p>
 */
public class ReglaNegocioException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Construye una instancia con los datos recibidos y valida sus invariantes. */
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
