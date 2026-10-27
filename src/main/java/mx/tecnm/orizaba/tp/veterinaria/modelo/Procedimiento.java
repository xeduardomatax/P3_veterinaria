package mx.tecnm.orizaba.tp.veterinaria.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase abstracta <strong>sellada</strong> que factoriza el estado y el comportamiento común de todo
 * procedimiento. Sus tres subtipos se declaran como clases anidadas estáticas finales
 * (véase {@code java_4_modelo/t1_abstraccion/Abstraccion04_anidadas}).
 * <ul>
 *   <li>Abstracción + Jerarquía: patrón <em>Template Method</em> ({@link #tarifa(int)} es final y delega en
 *       {@link #calcularTarifa(int)}).</li>
 *   <li>Proceso: bloque estático, bloque de instancia y encadenamiento {@code this(...)}/{@code super(...)}
 *       (véase {@code java_2_proceso/Proceso03}, {@code Proceso09}).</li>
 *   <li>Persistencia: {@code Serializable} con un campo {@code transient}
 *       (véase {@code java_4_modelo/t7_persistencia/a_serializacion/NoSerializar_09}).</li>
 * </ul>
 */
public abstract sealed class Procedimiento implements Facturable, Serializable
        permits Procedimiento.Vacuna, Procedimiento.Consulta, Procedimiento.Cirugia {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final AtomicInteger INSTANCIAS_CREADAS;

    static {
        INSTANCIAS_CREADAS = new AtomicInteger();
    }

    private final CodigoClinico clave;
    private String nombre;
    private double precioBase;
    private final LocalDateTime creadoEn;
    /** No se serializa: tras recuperar un respaldo vuelve a 0. */
    private transient int cotizaciones;

    {
        // Bloque de inicialización de instancia: se ejecuta antes del cuerpo de cualquier constructor.
        creadoEn = LocalDateTime.now();
        INSTANCIAS_CREADAS.incrementAndGet();
    }

    /** Constructor para objetos nuevos: genera la clave y encadena con {@code this(...)}. */
    protected Procedimiento(String prefijo, String nombre, double precioBase) {
        this(CodigoClinico.siguiente(prefijo), nombre, precioBase);
    }

    /** Constructor para objetos reconstruidos (persistencia) con clave conocida. */
    protected Procedimiento(CodigoClinico clave, String nombre, double precioBase) {
        this.clave = Objects.requireNonNull(clave);
        CodigoClinico.registrar(clave);
        setNombre(nombre);
        setPrecioBase(precioBase);
    }

    /** Devuelve el contador de construcciones de procedimientos. */
    public static int instanciasCreadas() {
        return INSTANCIAS_CREADAS.get();
    }

    /** Devuelve la clave clínica de identidad. */
    @Override
    public final CodigoClinico clave() {
        return clave;
    }

    /** Devuelve el nombre validado. */
    @Override
    public final String nombre() {
        return nombre;
    }

    /** Devuelve el precio base sin descuento ni impuestos. */
    @Override
    public final double precioBase() {
        return precioBase;
    }

    /** Devuelve el instante de creación del procedimiento. */
    public final LocalDateTime creadoEn() {
        return creadoEn;
    }

    /** Devuelve las cotizaciones desde la creación o deserialización. */
    public final int cotizaciones() {
        return cotizaciones;
    }

    /** Valida y asigna un nombre no vacío. */
    public final void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre es obligatorio");
        this.nombre = nombre.strip();
    }

    /** Valida y asigna un precio finito no negativo. */
    public final void setPrecioBase(double precioBase) {
        if (!Double.isFinite(precioBase) || precioBase < 0) throw new ReglaNegocioException("El precio debe ser finito y no negativo");
        this.precioBase = precioBase;
    }

    /** Método plantilla: valida, contabiliza y redondea; la variación la aporta cada subtipo. */
    @Override
    public final double tarifa(int unidades) {
        if (unidades <= 0) throw new ReglaNegocioException("Las sesiones deben ser positivas");
        cotizaciones++;
        return Math.round(calcularTarifa(unidades) * 100.0) / 100.0;
    }

    /** Punto de variación del Template Method. */
    protected abstract double calcularTarifa(int unidades);

    /** Copia con clave nueva; los subtipos la redefinen con <strong>retorno covariante</strong>. */
    public abstract Procedimiento copiar();

    /** Nombre corto del subtipo (útil para la UI y la persistencia). */
    public final String tipo() {
        return getClass().getSimpleName();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Procedimiento otro && clave.equals(otro.clave));
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return clave.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s[%s, %s, $%.2f]", tipo(), clave, nombre, precioBase);
    }

    /**
     * Vacuna: precio por dosis.
     * <p>Subtipo de Procedimiento declarado como clase anidada estática final; implementa Programable.</p>
     */
    public static final class Vacuna extends Procedimiento implements Programable {
        @Serial
        private static final long serialVersionUID = 1L;

        public static final String PREFIJO = "VAC";

        private final String biologico;

        /** Construye una instancia con los datos recibidos y valida sus invariantes. */
        public Vacuna(String nombre, double precioBase, String biologico) {
            super(PREFIJO, nombre, precioBase);
            this.biologico = validar(biologico);
        }

        /** Construye una instancia con los datos recibidos y valida sus invariantes. */
        public Vacuna(CodigoClinico clave, String nombre, double precioBase, String biologico) {
            super(clave, nombre, precioBase);
            this.biologico = validar(biologico);
        }

        private static String validar(String biologico) {
            if (biologico == null || biologico.isBlank()) throw new ReglaNegocioException("El biológico es obligatorio");
            return biologico.strip();
        }

        /** Devuelve el biológico de la vacuna. */
        public String biologico() {
            return biologico;
        }

        @Override
        protected double calcularTarifa(int unidades) {
            return precioBase() * unidades;
        }

        /** Retorno covariante: devuelve Vacuna, no Procedimiento. */
        @Override
        public Vacuna copiar() {
            return new Vacuna(nombre(), precioBase(), biologico);
        }

        /** Devuelve el intervalo de seguimiento en días. */
        @Override
        public int intervaloDias() {
            return 365;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return super.toString() + " {biológico=" + biologico + "}";
        }
    }

    /**
     * Consulta: 25 % adicional por cada nivel de prioridad.
     * <p>Subtipo de Procedimiento declarado como clase anidada estática final; implementa Supervisado.</p>
     */
    public static final class Consulta extends Procedimiento implements Supervisado {
        @Serial
        private static final long serialVersionUID = 1L;

        public static final String PREFIJO = "CON";

        private final int prioridad;

        /** Construye una instancia con los datos recibidos y valida sus invariantes. */
        public Consulta(String nombre, double precioBase, int prioridad) {
            super(PREFIJO, nombre, precioBase);
            this.prioridad = validar(prioridad);
        }

        /** Construye una instancia con los datos recibidos y valida sus invariantes. */
        public Consulta(CodigoClinico clave, String nombre, double precioBase, int prioridad) {
            super(clave, nombre, precioBase);
            this.prioridad = validar(prioridad);
        }

        private static int validar(int prioridad) {
            if (prioridad < 1 || prioridad > 3) throw new ReglaNegocioException("La prioridad debe estar entre 1 y 3");
            return prioridad;
        }

        /** Devuelve la prioridad validada de la consulta. */
        public int prioridad() {
            return prioridad;
        }

        @Override
        protected double calcularTarifa(int unidades) {
            return precioBase() * unidades * (1 + 0.25 * (prioridad - 1));
        }

        /** Retorno covariante: devuelve Consulta, no Procedimiento. */
        @Override
        public Consulta copiar() {
            return new Consulta(nombre(), precioBase(), prioridad);
        }

        /** Devuelve el responsable de supervisión. */
        @Override
        public String responsable() {
            return prioridad == 3 ? "Médico de guardia" : "Médico general";
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return super.toString() + " {prioridad=" + prioridad + "}";
        }
    }

    /**
     * Cirugía: sesiones posteriores a la segunda al 60 %.
     * <p>Subtipo de Procedimiento declarado como clase anidada estática final; implementa Programable, Supervisado.</p>
     */
    public static final class Cirugia extends Procedimiento implements Programable, Supervisado {
        @Serial
        private static final long serialVersionUID = 1L;

        public static final String PREFIJO = "CIR";

        private final int horasQuirofano;

        /** Construye una instancia con los datos recibidos y valida sus invariantes. */
        public Cirugia(String nombre, double precioBase, int horasQuirofano) {
            super(PREFIJO, nombre, precioBase);
            this.horasQuirofano = validar(horasQuirofano);
        }

        /** Construye una instancia con los datos recibidos y valida sus invariantes. */
        public Cirugia(CodigoClinico clave, String nombre, double precioBase, int horasQuirofano) {
            super(clave, nombre, precioBase);
            this.horasQuirofano = validar(horasQuirofano);
        }

        private static int validar(int horasQuirofano) {
            if (horasQuirofano <= 0) throw new ReglaNegocioException("Las horas de quirófano deben ser positivas");
            return horasQuirofano;
        }

        /** Devuelve las horas de quirófano programadas. */
        public int horasQuirofano() {
            return horasQuirofano;
        }

        @Override
        protected double calcularTarifa(int unidades) {
            return unidades <= 2 ? precioBase() * unidades : precioBase() * 2 + (unidades - 2) * precioBase() * 0.6;
        }

        /** Retorno covariante: devuelve Cirugia, no Procedimiento. */
        @Override
        public Cirugia copiar() {
            return new Cirugia(nombre(), precioBase(), horasQuirofano);
        }

        /** Devuelve el intervalo de seguimiento en días. */
        @Override
        public int intervaloDias() {
            return 15;
        }

        /** Devuelve el responsable de supervisión. */
        @Override
        public String responsable() {
            return "Cirujano titular";
        }

        /** Resolución explícita de la colisión entre métodos por defecto. */
        @Override
        public String descripcionCorta() {
            return Programable.super.descripcionCorta() + " | " + Supervisado.super.descripcionCorta();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return super.toString() + " {horas de quirófano=" + horasQuirofano + "}";
        }
    }
}
