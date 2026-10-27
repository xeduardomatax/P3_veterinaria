package mx.tecnm.orizaba.tp.veterinaria.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Administra la conexión JDBC con SQLite y el esquema de la base de datos.
 * <p>Proceso: el bloque estático carga el controlador la primera vez que la clase se inicializa
 * (véase {@code java_2_proceso/Proceso07}: uso activo de una clase).</p>
 */
public final class ConexionSQLite implements AutoCloseable {

    private static final String[] ESQUEMA = {
        """
        CREATE TABLE IF NOT EXISTS procedimiento (
            clave        TEXT PRIMARY KEY,
            tipo         TEXT NOT NULL,
            nombre       TEXT NOT NULL,
            precio_base  REAL NOT NULL CHECK (precio_base >= 0),
            atributo     TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS propietario (
            id      INTEGER PRIMARY KEY,
            nombre  TEXT NOT NULL,
            correo  TEXT NOT NULL UNIQUE,
            nivel   TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS atencion (
            id        INTEGER PRIMARY KEY,
            propietario_id  INTEGER NOT NULL REFERENCES propietario(id),
            fecha     TEXT NOT NULL,
            estado    TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS atencion_linea (
            atencion_id   INTEGER NOT NULL REFERENCES atencion(id) ON DELETE CASCADE,
            clave_procedimiento TEXT NOT NULL REFERENCES procedimiento(clave),
            unidades        INTEGER NOT NULL CHECK (unidades > 0),
            PRIMARY KEY (atencion_id, clave_procedimiento)
        )"""
    };

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("No se encontró el controlador sqlite-jdbc en el classpath");
        }
    }

    private final Connection conexion;

    /** @param rutaArchivo archivo .db (se crea si no existe) */
    public ConexionSQLite(String rutaArchivo) {
        try {
            conexion = DriverManager.getConnection("jdbc:sqlite:" + rutaArchivo);
            try (Statement s = conexion.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
                for (String ddl : ESQUEMA) s.execute(ddl);
            }
        } catch (SQLException e) { throw new PersistenciaException("No se pudo abrir SQLite", e); }
    }

    /** Devuelve la conexión administrada para uso exclusivo de los repositorios. */
    public Connection conexion() {
        return conexion;
    }

    /**
     * Ejecuta un trabajo dentro de una transacción: confirma si termina bien y revierte ante error.
     */
    public <R> R enTransaccion(TrabajoSQL<R> trabajo) {
        synchronized (conexion) {
            try {
                conexion.setAutoCommit(false);
                try {
                    R resultado = trabajo.ejecutar(conexion);
                    conexion.commit();
                    return resultado;
                } catch (SQLException | RuntimeException e) {
                    try { conexion.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
                    throw new PersistenciaException("No se pudo completar la transacción", e);
                } finally { conexion.setAutoCommit(true); }
            } catch (SQLException e) { throw new PersistenciaException("Error de transacción", e); }
        }
    }

    /** Interfaz funcional miembro: trabajo que puede lanzar {@link SQLException}. */
    @FunctionalInterface
    public interface TrabajoSQL<R> {
        /** Ejecuta el trabajo transaccional en la conexión recibida. */
        R ejecutar(Connection c) throws SQLException;
    }

    /** Libera los recursos administrados y detiene el monitor cuando corresponde. */
    @Override
    public void close() {
        try {
            conexion.close();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al cerrar la conexión", e);
        }
    }
}
