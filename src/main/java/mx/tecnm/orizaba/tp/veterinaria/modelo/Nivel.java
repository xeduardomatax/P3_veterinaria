package mx.tecnm.orizaba.tp.veterinaria.modelo;

/**
 * Nivel del propietario; cada constante encapsula su etiqueta y su descuento.
 * <p>Tipificación fuerte: un {@code enum} es un tipo cerrado con estado y comportamiento.</p>
 */
public enum Nivel {
    GENERAL("General", 0.0),
    PLAN_SALUD("Plan de salud", 0.12),
    RESCATISTA("Rescatista", 0.3);

    private final String etiqueta;
    private final double descuento;

    Nivel(String etiqueta, double descuento) {
        this.etiqueta = etiqueta;
        this.descuento = descuento;
    }

    /** Devuelve la etiqueta para presentación. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Devuelve la fracción de descuento del nivel. */
    public double descuento() {
        return descuento;
    }

    /** Aplica el descuento del nivel a un monto, redondeando a 2 decimales. */
    public double aplicar(double monto) {
        return Math.round(monto * (1 - descuento) * 100.0) / 100.0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return etiqueta;
    }
}
