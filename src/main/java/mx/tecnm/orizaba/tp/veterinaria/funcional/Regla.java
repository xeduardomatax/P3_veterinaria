package mx.tecnm.orizaba.tp.veterinaria.funcional;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Interfaz funcional propia (un solo método abstracto) con métodos por defecto de composición
 * (véase {@code java_5_lambdas/b_interfaces_funcionales} y {@code d_orden_superior/Superior2}).
 *
 * @param <T> tipo evaluado
 */
@FunctionalInterface
public interface Regla<T> {

    /** Evalúa la regla para el valor recibido. */
    boolean evaluar(T t);

    /** Combina dos reglas mediante conjunción con cortocircuito. */
    default Regla<T> y(Regla<? super T> otra) {
        Objects.requireNonNull(otra);
        return t -> evaluar(t) && otra.evaluar(t);
    }

    /** Combina dos reglas mediante disyunción con cortocircuito. */
    default Regla<T> o(Regla<? super T> otra) {
        Objects.requireNonNull(otra);
        return t -> evaluar(t) || otra.evaluar(t);
    }

    /** Devuelve la negación lógica de esta regla. */
    default Regla<T> negar() {
        return t -> !evaluar(t);
    }

    /** Adaptador hacia {@link Predicate} mediante referencia a método de instancia particular. */
    default Predicate<T> comoPredicado() {
        return this::evaluar;
    }

    /** Crea una regla que acepta cualquier valor. */
    static <T> Regla<T> todas() {
        return t -> true;
    }

    /** Adaptador desde {@link Predicate}. */
    static <T> Regla<T> de(Predicate<? super T> p) {
        return p::test;
    }
}
