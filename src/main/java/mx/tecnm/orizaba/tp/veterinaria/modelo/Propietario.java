package mx.tecnm.orizaba.tp.veterinaria.modelo;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Propietario del sistema. Implementa {@link Externalizable}: el propio objeto decide qué y cómo se
 * escribe (véase {@code java_4_modelo/t7_persistencia/b_externalizacion/Cosas2}); por ello requiere un
 * constructor público sin argumentos.
 */
public class Propietario implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private int id;
    private String nombre;
    private String correo;
    private Nivel nivel;

    /** Requerido por la externalización: se invoca antes de readExternal. */
    public Propietario() {
    }

    /** Construye una instancia con los datos recibidos y valida sus invariantes. */
    public Propietario(int id, String nombre, String correo, Nivel nivel) {
        if (id <= 0) throw new ReglaNegocioException("El identificador debe ser positivo");
        this.id = id;
        setNombre(nombre);
        setCorreo(correo);
        setNivel(nivel);
    }

    /** Encadenamiento de constructores: nivel por omisión. */
    public Propietario(int id, String nombre, String correo) {
        this(id, nombre, correo, Nivel.values()[0]);
    }

    /** Devuelve el identificador de la entidad. */
    public int id() {
        return id;
    }

    /** Devuelve el nombre validado. */
    public String nombre() {
        return nombre;
    }

    /** Devuelve el correo normalizado. */
    public String correo() {
        return correo;
    }

    /** Devuelve el nivel que determina el descuento. */
    public Nivel nivel() {
        return nivel;
    }

    /** Valida y asigna un nombre no vacío. */
    public final void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre es obligatorio");
        this.nombre = nombre.strip();
    }

    /** Valida el correo y lo almacena en minúsculas. */
    public final void setCorreo(String correo) {
        if (correo == null || !CORREO.matcher(correo.strip()).matches()) throw new ReglaNegocioException("Correo inválido");
        this.correo = correo.strip().toLowerCase(java.util.Locale.ROOT);
    }

    /** Asigna un nivel no nulo. */
    public final void setNivel(Nivel nivel) {
        this.nivel = Objects.requireNonNull(nivel, "nivel");
    }

    /** Escribe identidad, nombre, correo y nivel en ese orden. */
    @Override
    public void writeExternal(ObjectOutput salida) throws IOException {
        salida.writeInt(id);
        salida.writeUTF(nombre);
        salida.writeUTF(correo);
        salida.writeUTF(nivel.name());
    }

    /** Reconstruye y valida los datos en el orden de escritura. */
    @Override
    public void readExternal(ObjectInput entrada) throws IOException {
        int leido = entrada.readInt();
        if (leido <= 0) throw new IOException("Identificador inválido");
        id = leido;
        setNombre(entrada.readUTF());
        setCorreo(entrada.readUTF());
        setNivel(Nivel.valueOf(entrada.readUTF()));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Propietario otro && id == otro.id);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s #%d %s <%s> (%s)", "Propietario", id, nombre, correo, nivel);
    }
}
