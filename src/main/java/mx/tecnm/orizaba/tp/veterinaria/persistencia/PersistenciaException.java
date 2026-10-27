package mx.tecnm.orizaba.tp.veterinaria.persistencia;

import java.io.Serial;

/** Envuelve los errores de acceso a datos en una excepción no comprobada del dominio. */
public class PersistenciaException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Construye una instancia con los datos recibidos y valida sus invariantes. */
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
