package mx.tecnm.orizaba.tp.veterinaria.funcional;

import mx.tecnm.orizaba.tp.veterinaria.modelo.Procedimiento;

/**
 * Fábrica genérica acotada, diseñada para recibir <strong>referencias a constructor</strong>:
 * {@code FabricaElemento<Procedimiento.Vacuna, String> f = Procedimiento.Vacuna::new;}
 * (véase {@code java_5_lambdas/c_referencias_metodos/Referencias4}).
 * <p>El límite {@code E extends Procedimiento} garantiza que la fábrica devuelve un
 * procedimiento, conservando el subtipo concreto sin conversiones. {@code A} representa
 * su atributo: {@code String} para Vacuna e {@code Integer} para Consulta y Cirugia
 * (con desempaquetado a {@code int} al invocar sus constructores).
 * La fachada {@code Clinica.registrar} recibe esta función y administra la persistencia.</p>
 *
 * @param <E> subtipo concreto de Procedimiento
 * @param <A> tipo del atributo específico del subtipo
 */
@FunctionalInterface
public interface FabricaElemento<E extends Procedimiento, A> {

    /** Construye un procedimiento con nombre, precio y atributo específico. */
    E crear(String nombre, double precioBase, A atributo);
}
