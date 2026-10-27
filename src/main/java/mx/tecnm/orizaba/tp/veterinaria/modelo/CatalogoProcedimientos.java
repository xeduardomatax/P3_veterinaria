package mx.tecnm.orizaba.tp.veterinaria.modelo;

import mx.tecnm.orizaba.tp.veterinaria.funcional.Regla;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * CatalogoProcedimientos: <strong>agregación</strong> de procedimientos (las partes existen con independencia del
 * todo; véase {@code java_4_modelo/t4_jerarquia/t2_agregacion/Jerarquia02}).
 * Reúne en un solo lugar tres variantes de tipos internos:
 * clase interna privada ({@link Iterador}), clase anónima ({@link #porTarifa(int)}) y clase local
 * ({@link #resumen()}).
 */
public class CatalogoProcedimientos implements Iterable<Procedimiento>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final LinkedHashMap<CodigoClinico, Procedimiento> elementos = new LinkedHashMap<>();

    /** Constructor con número variable de argumentos. */
    public CatalogoProcedimientos(Procedimiento... iniciales) {
        agregar(iniciales);
    }

    /** Agrega procedimientos no nulos con claves únicas. */
    public final void agregar(Procedimiento... nuevos) {
        for (Procedimiento e : nuevos) {
            if (e == null) throw new mx.tecnm.orizaba.tp.veterinaria.modelo.ReglaNegocioException("Procedimiento nulo");
            if (elementos.putIfAbsent(e.clave(), e) != null) throw new ReglaNegocioException("Clave duplicada: " + e.clave());
        }
    }

    /** Busca entidades por identidad o criterio, según el contrato. */
    public Optional<Procedimiento> buscar(CodigoClinico clave) {
        return Optional.ofNullable(elementos.get(clave));
    }

    /** Retira un procedimiento; en la fachada impide retirar los utilizados. */
    public boolean retirar(CodigoClinico clave) {
        return elementos.remove(clave) != null;
    }

    /** Devuelve la cantidad de procedimientos del catálogo. */
    public int tamanio() {
        return elementos.size();
    }

    /** Filtrado con la interfaz funcional propia {@link Regla}. */
    public List<Procedimiento> filtrar(Regla<? super Procedimiento> regla) {
        return elementos.values().stream().filter(regla::evaluar).toList();
    }

    /** Filtrado por subtipo con genéricos acotados: {@code deTipo(Procedimiento.Vacuna.class)}. */
    public <E extends Procedimiento> List<E> deTipo(Class<E> tipo) {
        return elementos.values().stream().filter(tipo::isInstance).map(tipo::cast).toList();
    }

    /** Devuelve una lista ordenada con el comparador recibido. */
    public List<Procedimiento> ordenados(Comparator<? super Procedimiento> criterio) {
        return elementos.values().stream().sorted(criterio).toList();
    }

    /**
     * Comparador implementado con una <strong>clase anónima</strong> que captura la variable
     * {@code unidades} (efectivamente final; véase {@code Parcel09}).
     */
    public static Comparator<Procedimiento> porTarifa(final int unidades) {
        return new Comparator<>() {
            @Override public int compare(Procedimiento a, Procedimiento b) {
                return Double.compare(a.tarifa(unidades), b.tarifa(unidades));
            }
        };
    }

    /** Resumen con clase local y captura efectivamente final (Parcel05 y Parcel09). */
    public String resumen() {
        String formato = "%d procedimientos; suma $%.2f; mínimo $%.2f; máximo $%.2f";
        class Acumulador {
            int cuenta; double suma; double minimo = Double.POSITIVE_INFINITY; double maximo;
            void agregar(Procedimiento e) { cuenta++; suma += e.precioBase(); minimo = Math.min(minimo, e.precioBase()); maximo = Math.max(maximo, e.precioBase()); }
            String resultado() {
                // Captura una variable local que no se reasigna, como exige C3.
                return String.format(formato, cuenta, suma, cuenta == 0 ? 0 : minimo, maximo);
            }
        }
        Acumulador a = new Acumulador();
        elementos.values().forEach(a::agregar);
        return a.resultado();
    }

    /** Devuelve un iterador sobre una instantánea inmodificable desde el cliente. */
    @Override
    public Iterator<Procedimiento> iterator() {
        return new Iterador();
    }

    /**
     * Clase interna privada que implementa una interfaz pública: el cliente sólo conoce
     * {@code Iterator<Procedimiento>} y la implementación queda oculta (véase {@code Parcel04}).
     * Itera sobre una instantánea, de modo que el catálogo no puede modificarse a través de ella.
     */
    private class Iterador implements Iterator<Procedimiento> {
        private final List<Procedimiento> instantanea = new ArrayList<>(elementos.values());
        private int cursor;

        /** Indica si quedan procedimientos en la instantánea. */
        @Override
        public boolean hasNext() {
            return cursor < instantanea.size();
        }

        /** Devuelve el siguiente procedimiento o lanza NoSuchElementException. */
        @Override
        public Procedimiento next() {
            if (!hasNext()) throw new NoSuchElementException();
            return instantanea.get(cursor++);
        }
    }
}
