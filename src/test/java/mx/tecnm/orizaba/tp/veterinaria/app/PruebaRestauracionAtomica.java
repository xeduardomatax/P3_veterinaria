package mx.tecnm.orizaba.tp.veterinaria.app;

import java.nio.file.Files;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Nivel;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Procedimiento;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.PersistenciaException;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.RepositorioProcedimientoSQLite;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;

/** Regresión: fallar en la segunda fila de una restauración no cambia SQLite ni la memoria. */
public final class PruebaRestauracionAtomica {
    private PruebaRestauracionAtomica() {}

    /** Ejecuta la restauración fallida y exitosa usando una base y un respaldo temporales. */
    public static void main(String[] args) throws Exception {
        var archivo = Files.createTempFile("catalogo-atomico-", ".bin");
        try (var bd = new ConexionSQLite(":memory:"); var c = new Clinica(bd)) {
            var consulta = c.registrar(Procedimiento.Consulta::new, "Consulta", 100, 1);
            var vacuna = c.registrar(Procedimiento.Vacuna::new, "Vacuna", 50, "Rabia");
            var propietario = c.registrarPropietario("Ana", "ana@example.com", Nivel.GENERAL);
            var atencion = c.abrir(propietario.id());
            c.agregarLinea(atencion.id(), consulta.clave().toString(), 1);
            c.exportarRespaldo(archivo);
            c.editarProcedimiento(consulta.clave().toString(), "Consulta actual", 200, "1");
            c.editarProcedimiento(vacuna.clave().toString(), "Vacuna actual", 80, "Rabia");
            // CON se escribe antes de VAC: falla después de haber ejecutado la primera fila.
            try (var s = bd.conexion().createStatement()) {
                s.executeUpdate("CREATE TRIGGER fallo BEFORE UPDATE ON procedimiento "
                        + "WHEN NEW.tipo = 'Vacuna' BEGIN SELECT RAISE(ABORT, 'Fallo simulado'); END");
            }
            try {
                c.restaurarCatalogo(archivo);
                throw new AssertionError("Se aceptó una restauración incompleta");
            } catch (PersistenciaException esperada) {
                var repo = new RepositorioProcedimientoSQLite(bd);
                verificar(repo.buscar(consulta.clave()).orElseThrow().precioBase() == 200,
                        "La primera fila no se revirtió en SQLite");
                verificar(repo.buscar(vacuna.clave()).orElseThrow().precioBase() == 80,
                        "La segunda fila cambió en SQLite");
                verificar(c.elemento(consulta.clave().toString()).precioBase() == 200
                        && c.registro(atencion.id()).total() == 200, "La memoria cambió antes del commit");
                verificar(bd.conexion().getAutoCommit(), "No se restauró autocommit");
            }
            try (var s = bd.conexion().createStatement()) { s.executeUpdate("DROP TRIGGER fallo"); }
            c.restaurarCatalogo(archivo);
            verificar(c.registro(atencion.id()).total() == 100, "No se reenlazó la atención restaurada");
            verificar(c.elemento(vacuna.clave().toString()).precioBase() == 50, "Restauración incompleta");
            System.out.println("Restauración: rollback de lote y memoria, commit y reenlace aprobados");
        } finally { Files.deleteIfExists(archivo); }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
