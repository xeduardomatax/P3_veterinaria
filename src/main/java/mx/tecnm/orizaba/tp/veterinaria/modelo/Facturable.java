package mx.tecnm.orizaba.tp.veterinaria.modelo;

import java.util.Comparator;

/**
 * Abstracción raíz del dominio: todo lo que puede facturarse expone una clave,
 * un nombre, un precio base y una tarifa por unidad (sesiones).
 * <p>Abstracción (Booch): define el <em>qué</em>, no el <em>cómo</em>
 * (véase {@code java_4_modelo/t1_abstraccion/Abstraccion03_interfaces}).</p>
 */
public interface Facturable {

    /** Constante de interfaz: implícitamente {@code public static final}. */
    double TASA_IMPUESTO = 0.16;

    /** Devuelve la clave clínica de identidad. */
    CodigoClinico clave();

    /** Devuelve el nombre validado. */
    String nombre();

    /** Devuelve el precio base sin descuento ni impuestos. */
    double precioBase();

    /** Tarifa (sin impuestos) para la cantidad de sesiones indicada. */
    double tarifa(int unidades);

    /** Método por defecto (extensión de la interfaz sin romper implementaciones). */
    default double tarifaConImpuesto(int unidades) {
        return Math.round(tarifa(unidades) * (1 + TASA_IMPUESTO) * 100.0) / 100.0;
    }

    /** Método estático de interfaz: comparador por nombre (referencia a método de objeto arbitrario). */
    static Comparator<Facturable> porNombre() {
        return Comparator.comparing(Facturable::nombre, String.CASE_INSENSITIVE_ORDER);
    }
}
