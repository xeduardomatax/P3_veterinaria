package mx.tecnm.orizaba.tp.veterinaria.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Atencion: agregado con ciclo de vida ({@link Estado}) compuesto por líneas.
 * <ul>
 *   <li>Composición: {@link Linea} es una clase <strong>interna</strong> (no estática) con constructor
 *       privado; sólo existe dentro de un(a) atención y accede a su envolvente con
 *       {@code Atencion.this} (véase {@code java_3_tipos_internos/t1_fundamentos/Parcel01..04}).</li>
 *   <li>Concurrencia: los métodos que cambian estado están sincronizados porque el monitor
 *       los consulta desde otro hilo.</li>
 *   <li>Persistencia: serializar un objeto de clase interna exige que la envolvente sea serializable
 *       (véase {@code SerializaClasesInternas_08}).</li>
 * </ul>
 */
public class Atencion implements Iterable<Atencion.Linea>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final int id;
    private final Propietario propietario;
    private final LocalDate fecha;
    private Estado estado;
    private final ArrayList<Linea> lineas = new ArrayList<>();

    {
        estado = Estado.ABIERTA;
    }

    /** Nuevo(a) atención con identificador consecutivo y fecha de hoy. */
    public Atencion(Propietario propietario) {
        this(SECUENCIA.incrementAndGet(), propietario, LocalDate.now());
    }

    /** Constructor completo (también usado al reconstruir desde la base de datos). */
    public Atencion(int id, Propietario propietario, LocalDate fecha) {
        if (id <= 0) throw new ReglaNegocioException("El identificador debe ser positivo");
        this.id = id;
        this.propietario = Objects.requireNonNull(propietario);
        this.fecha = Objects.requireNonNull(fecha);
        SECUENCIA.accumulateAndGet(id, Math::max);
    }

    /** Devuelve el identificador de la entidad. */
    public int id() {
        return id;
    }

    /** Obtiene el propietario asociado o identificado. */
    public Propietario propietario() {
        return propietario;
    }

    /** Devuelve la fecha de registro. */
    public LocalDate fecha() {
        return fecha;
    }

    /** Devuelve el estado actual de la atención. */
    public synchronized Estado estado() {
        return estado;
    }

    /** Agrega una línea; sólo es posible mientras está ABIERTA y sin repetir procedimiento. */
    public synchronized Linea agregar(Procedimiento elemento, int unidades) {
        if (estado != Estado.ABIERTA) throw new ReglaNegocioException("Sólo se agregan líneas a atenciones registradas");
        Objects.requireNonNull(elemento);
        if (lineas.stream().anyMatch(l -> l.elemento.equals(elemento))) throw new ReglaNegocioException("El procedimiento ya está agregado");
        Linea linea = new Linea(elemento, unidades);
        lineas.add(linea);
        return linea;
    }

    /** Quita la línea del procedimiento indicado (sólo en estado ABIERTA). */
    public synchronized boolean quitar(CodigoClinico clave) {
        if (estado != Estado.ABIERTA) throw new ReglaNegocioException("Sólo se quitan líneas a atenciones registradas");
        return lineas.removeIf(l -> l.elemento.clave().equals(clave));
    }

    /** Valida y aplica la transición; rechaza transiciones inválidas sin alterar el estado. */
    public synchronized void cambiarEstado(Estado destino) {
        if (!estado.permiteTransicionA(destino)) throw new ReglaNegocioException("Transición no permitida: " + estado + " → " + destino);
        if (destino == Estado.EN_CURSO && lineas.isEmpty()) throw new ReglaNegocioException("Agregue al menos un procedimiento");
        estado = destino;
    }

    /** Inicia la atención; exige al menos una línea y estado registrado. */
    public void iniciar() {
        cambiarEstado(Estado.EN_CURSO);
    }

    /** Concluye la atención en curso. */
    public void cerrar() {
        cambiarEstado(Estado.CERRADA);
    }

    /** Cancela una atención que no sea terminal. */
    public void cancelar() {
        cambiarEstado(Estado.CANCELADA);
    }

    /** Uso exclusivo de la capa de persistencia al reconstruir el objeto. */
    public synchronized void restaurarEstado(Estado estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    /** Devuelve una instantánea inmodificable de las líneas. */
    public synchronized List<Linea> lineas() {
        return Collections.unmodifiableList(new ArrayList<>(lineas));
    }

    /** Suma de subtotales sin descuento. */
    public synchronized double subtotal() {
        return redondear(lineas.stream().mapToDouble(Linea::subtotal).sum());
    }

    /** Total con el descuento del nivel del propietario. */
    public synchronized double total() {
        return redondear(lineas.stream().mapToDouble(Linea::importe).sum());
    }

    /** Calcula los días transcurridos desde la fecha de la atención. */
    public long diasDesde(LocalDate hoy) {
        return ChronoUnit.DAYS.between(fecha, hoy);
    }

    /** Devuelve un iterador sobre una instantánea inmodificable desde el cliente. */
    @Override
    public Iterator<Linea> iterator() {
        return lineas().iterator();
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Atencion #%d [%s] %s, %d línea(s), total $%.2f",
                id, estado(), propietario.nombre(), lineas().size(), total());
    }

    /**
     * Línea de atención: clase interna (composición). Su constructor es privado, de modo que
     * sólo la envolvente puede crearla; accede al estado de la envolvente con {@code Atencion.this}.
     */
    public final class Linea implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final Procedimiento elemento;
        private final int unidades;

        private Linea(Procedimiento elemento, int unidades) {
            if (unidades <= 0) throw new ReglaNegocioException("Las sesiones deben ser positivas");
            this.elemento = Objects.requireNonNull(elemento);
            this.unidades = unidades;
        }

        /** Obtiene el procedimiento asociado o identificado. */
        public Procedimiento elemento() {
            return elemento;
        }

        /** Devuelve las sesiones de la línea. */
        public int unidades() {
            return unidades;
        }

        /** Calcula la tarifa polimórfica de las sesiones de esta línea. */
        public double subtotal() {
            return elemento.tarifa(unidades);
        }

        /** Importe con el descuento del nivel del propietario de la envolvente. */
        public double importe() {
            return Atencion.this.propietario.nivel().aplicar(subtotal());
        }

        /** Devuelve la atención envolvente de esta línea. */
        public Atencion contenedor() {
            return Atencion.this;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return String.format("%s x %d sesiones = $%.2f", elemento.nombre(), unidades, importe());
        }
    }
}
