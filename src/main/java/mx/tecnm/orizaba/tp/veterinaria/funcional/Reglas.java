package mx.tecnm.orizaba.tp.veterinaria.funcional;

import mx.tecnm.orizaba.tp.veterinaria.modelo.Procedimiento;

/**
 * Biblioteca de reglas reutilizables (clase utilitaria no instanciable).
 */
public final class Reglas {

    private Reglas() {
        throw new AssertionError("No instanciable");
    }

    /** Crea una regla que acepta precios menores o iguales al máximo. */
    public static Regla<Procedimiento> precioHasta(double maximo) {
        return e -> e.precioBase() <= maximo;
    }

    /** Crea una búsqueda parcial de nombre insensible a mayúsculas. */
    public static Regla<Procedimiento> nombreContiene(String texto) {
        String consulta = java.util.Objects.requireNonNull(texto).toLowerCase(java.util.Locale.ROOT);
        return e -> e.nombre().toLowerCase(java.util.Locale.ROOT).contains(consulta);
    }

    /** Elementos que implementan una capacidad (p. ej. {@code Programable.class}). */
    public static Regla<Procedimiento> conCapacidad(Class<?> capacidad) {
        return capacidad::isInstance;
    }
}
