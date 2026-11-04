package mx.tecnm.orizaba.tp.veterinaria.modelo;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objeto valor inmutable que identifica a un procedimiento (formato {@code ABC-0001}).
 * <ul>
 *   <li>Encapsulamiento: {@code record} con constructor compacto que valida invariantes.</li>
 *   <li>Proceso: bloque de inicialización estática (véase {@code java_2_proceso/Proceso03}).</li>
 *   <li>Concurrencia: secuencias atómicas seguras entre hilos.</li>
 * </ul>
 */
public record CodigoClinico(String prefijo, int numero) implements Comparable<CodigoClinico>, Serializable {

    private static final Map<String, AtomicInteger> SECUENCIAS;

    static {
        // Inicialización estática: se ejecuta una sola vez, al primer uso activo de la clase.
        SECUENCIAS = new ConcurrentHashMap<>();
    }

    /** Valida el prefijo de tres mayúsculas y un número de cuatro dígitos. */
    public CodigoClinico {
        if (prefijo == null || !prefijo.matches("[A-Z]{3}") || (numero <= 0 || numero > 9999)) throw new IllegalArgumentException("Código clínico inválido: se requieren cuatro dígitos (0001 a 9999)");
    }

    /** Genera la siguiente clave del prefijo indicado (operación atómica). */
    public static CodigoClinico siguiente(String prefijo) {
        if (prefijo == null || !prefijo.matches("[A-Z]{3}")) throw new IllegalArgumentException("Prefijo inválido");
        int numero = SECUENCIAS.computeIfAbsent(prefijo, k -> new AtomicInteger())
                .updateAndGet(anterior -> {
                    if (anterior >= 9999) throw new IllegalStateException("Se agotaron las claves del prefijo " + prefijo);
                    return anterior + 1;
                });
        return new CodigoClinico(prefijo, numero);
    }

    /** Ajusta la secuencia para que nunca genere una clave ya existente (p. ej., tras leer la BD). */
    public static void registrar(CodigoClinico clave) {
        SECUENCIAS.computeIfAbsent(clave.prefijo(), k -> new AtomicInteger()).accumulateAndGet(clave.numero(), Math::max);
    }

    /** Convierte un texto {@code ABC-0001} en una clave. */
    public static CodigoClinico parse(String texto) {
        if (texto == null || !texto.matches("[A-Z]{3}-[0-9]{4}")) throw new IllegalArgumentException("Formato esperado: ABC-0001");
        return new CodigoClinico(texto.substring(0, 3), Integer.parseInt(texto.substring(4)));
    }

    /** Compara por prefijo y después por número. */
    @Override
    public int compareTo(CodigoClinico otra) {
        int c = prefijo.compareTo(otra.prefijo);
        return c == 0 ? Integer.compare(numero, otra.numero) : c;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s-%04d", prefijo, numero);
    }
}
