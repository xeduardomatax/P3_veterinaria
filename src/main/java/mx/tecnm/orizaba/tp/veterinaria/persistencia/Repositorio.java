package mx.tecnm.orizaba.tp.veterinaria.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de repositorio (patrón Repository).
 * Contiene un <strong>tipo miembro de interfaz</strong> ({@link MapeadorFila}), implícitamente
 * {@code public static} (véase {@code java_3_tipos_internos/t2_jerarquias/J05_Tipos_Miembro_Interfaz}).
 *
 * @param <T> entidad
 * @param <K> clave
 */
public interface Repositorio<T, K> {

    /** Inserta o actualiza la entidad mediante sentencia parametrizada. */
    void guardar(T entidad);

    Optional<T> buscar(K clave);

    List<T> todos();

    /** Elimina la entidad identificada y devuelve si existía. */
    boolean eliminar(K clave);

    /** Indica si la clave tiene una entidad persistida. */
    default boolean existe(K clave) {
        return buscar(clave).isPresent();
    }

    /** Guarda cada entidad del iterable mediante el contrato del repositorio. */
    default void guardarTodos(Iterable<? extends T> entidades) {
        entidades.forEach(this::guardar);
    }

    /** Convierte la fila actual de un {@link ResultSet} en un objeto. */
    @FunctionalInterface
    interface MapeadorFila<R> {
        /** Reconstruye una entidad desde la fila actual del resultado SQL. */
        R mapear(ResultSet rs) throws SQLException;
    }
}
