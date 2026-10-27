package mx.tecnm.orizaba.tp.veterinaria.eventos;

/**
 * Observador funcional de eventos. La UI se suscribe con una lambda, una referencia a método
 * o una clase anónima.
 * <p><strong>Importante:</strong> el monitor notifica desde un hilo de fondo; la UI debe
 * reenviar la actualización a su propio hilo ({@code Platform.runLater},
 * {@code SwingUtilities.invokeLater}, SSE/WebSocket, etc.).</p>
 */
@FunctionalInterface
public interface OyenteEvento {

    /** Recibe un evento del dominio en el hilo que lo publica. */
    void notificar(Evento evento);

    /** Composición secuencial de oyentes (función de orden superior). */
    default OyenteEvento luego(OyenteEvento siguiente) {
        java.util.Objects.requireNonNull(siguiente);
        return e -> { notificar(e); siguiente.notificar(e); };
    }
}
