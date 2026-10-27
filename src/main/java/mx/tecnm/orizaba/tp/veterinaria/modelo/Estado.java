package mx.tecnm.orizaba.tp.veterinaria.modelo;

/**
 * Ciclo de vida de un(a) atención: máquina de estados finita.
 * <pre>ABIERTA → EN_CURSO → CERRADA;  ABIERTA|EN_CURSO → CANCELADA</pre>
 */
public enum Estado {
    ABIERTA("Registrada"), EN_CURSO("En atención"), CERRADA("Concluida"), CANCELADA("Cancelada");

    private final String etiqueta;

    Estado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Devuelve la etiqueta para presentación. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Indica si la transición hacia {@code destino} es válida (switch como expresión). */
    public boolean permiteTransicionA(Estado destino) {
        return switch (this) {
            case ABIERTA -> destino == EN_CURSO || destino == CANCELADA;
            case EN_CURSO -> destino == CERRADA || destino == CANCELADA;
            case CERRADA, CANCELADA -> false;
        };
    }

    /** Indica si el estado impide transiciones posteriores. */
    public boolean esTerminal() {
        return this == CERRADA || this == CANCELADA;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return etiqueta;
    }
}
