package mx.tecnm.orizaba.tp.veterinaria.app;

import mx.tecnm.orizaba.tp.veterinaria.modelo.*;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.*;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

/** Pruebas adicionales de CRUD, reportes, respaldo y atomicidad SQLite. */
public final class PruebasIntegracion {
    private PruebasIntegracion() {}
    private static void verificar(boolean ok,String mensaje) { if (!ok) throw new AssertionError(mensaje); }
    private static void rechazar(Runnable tarea) {
        try { tarea.run(); } catch (ReglaNegocioException | IllegalArgumentException e) { return; }
        throw new AssertionError("La operación inválida fue aceptada");
    }
    /** Ejecuta los escenarios en bases temporales independientes de datos.db. */
    public static void main(String[] args) throws Exception {
        var temporal = Files.createTempDirectory("integracion-clinica");
        var fecha = new AtomicReference<>(LocalDate.of(2026,10,16));
        try (ConexionSQLite bd = new ConexionSQLite(temporal.resolve("clinica.db").toString()); Clinica c = new Clinica(bd,fecha::get)) {
            var p = c.registrarPropietario("Ana","ANA@example.com",Nivel.PLAN_SALUD);
            var otro = c.registrarPropietario("Luis","luis@example.com",Nivel.GENERAL);
            rechazar(() -> c.actualizarPropietario(p.id(),"Cambio","luis@example.com",Nivel.GENERAL));
            verificar(p.nombre().equals("Ana"),"Actualización inválida dejó cambios");
            c.actualizarPropietario(p.id(),"Ana López","ana@example.com",Nivel.RESCATISTA);
            var vacuna = c.registrar(Procedimiento.Vacuna::new,"Vacuna",100,"Rabia");
            var t = c.abrir(p.id()); c.agregarLinea(t.id(),vacuna.clave().toString(),2);
            c.editarProcedimiento(vacuna.clave().toString(),"Vacuna editada",200,"Polivalente");
            verificar(c.registro(t.id()).total() == 280,"Edición no reenlazó las líneas");
            rechazar(() -> c.editarProcedimiento(vacuna.clave().toString(),"",-1,""));
            verificar(c.elemento(vacuna.clave().toString()).precioBase() == 200,"Edición inválida alteró precio");
            c.iniciar(t.id()); c.cerrar(t.id());
            verificar(c.masRentables(1).getFirst().getValue() == 280,"Ranking incorrecto");
            verificar(c.particionarAtenciones().get(true).size() == 1,"Partición incorrecta");
            verificar(c.estadisticasTarifas().getAverage() == 200,"Estadística incorrecta");
            rechazar(() -> c.eliminarPropietario(p.id()));
            verificar(c.eliminarPropietario(otro.id()),"No eliminó propietario libre");
            var respaldo = temporal.resolve("catalogo.bin"); c.exportarRespaldo(respaldo);
            c.editarProcedimiento(vacuna.clave().toString(),"Temporal",250,"Temporal");
            c.restaurarCatalogo(respaldo);
            verificar(c.registro(t.id()).total() == 280,"Restauración incorrecta");
            var vacia = c.abrir(p.id());
            verificar(c.revisar() == 0,"Cancelación prematura");
            fecha.set(fecha.get().plusDays(1));
            verificar(c.revisar() == 1 && c.registro(vacia.id()).estado() == Estado.CANCELADA,"Límite de un día incorrecto");
            try {
                bd.enTransaccion(cx -> {
                    try (var s = cx.createStatement()) {
                        s.executeUpdate("INSERT INTO propietario VALUES(999,'Rollback','rollback@example.com','GENERAL')");
                        s.executeUpdate("INSERT INTO atencion VALUES(999,99999,'2026-10-16','ABIERTA')");
                    }
                    return null;
                });
                throw new AssertionError("Se aceptó una referencia inexistente");
            } catch (PersistenciaException esperada) {
                verificar(new RepositorioPropietarioSQLite(bd).buscar(999).isEmpty(),"Falló rollback");
                verificar(bd.conexion().getAutoCommit(),"No restauró autocommit");
            }
            verificar(c.eliminarAtencion(t.id()),"No eliminó atención");
            verificar(c.retirar(vacuna.clave()),"No eliminó procedimiento libre");
        }
        try (Clinica c = Clinica.abrirArchivo(temporal.resolve("clinica.db"))) {
            verificar(c.catalogo().tamanio() == 0 && c.registros().size() == 1,"Reinicio tras CRUD incorrecto");
        }
        verificarFalloEscritura();
        System.out.println("Integración: CRUD, reportes, respaldo, límites del monitor y rollback aprobados");
    }

    // El cierre anticipado es deliberado: simula una escritura sobre una conexión cerrada.
    @SuppressWarnings("try")
    private static void verificarFalloEscritura() {
        try (ConexionSQLite bd = new ConexionSQLite(":memory:"); Clinica c = new Clinica(bd)) {
            var p = c.registrarPropietario("Ana", "ana@example.com", Nivel.GENERAL);
            var v = c.registrar(Procedimiento.Vacuna::new, "Vacuna", 100, "Rabia");
            var consulta = c.registrar(Procedimiento.Consulta::new, "Consulta", 200, 1);
            var t = c.abrir(p.id());
            c.agregarLinea(t.id(), v.clave().toString(), 2);
            c.agregarLinea(t.id(), consulta.clave().toString(), 1);
            // Simula un fallo real de almacenamiento sin alterar datos.db.
            bd.close();
            try {
                c.quitarLinea(t.id(), v.clave().toString());
                throw new AssertionError("No se detectó la escritura fallida");
            } catch (PersistenciaException esperada) {
                verificar(t.lineas().size() == 2 && t.total() == 400,
                        "La escritura fallida quitó una línea en memoria");
                verificar(t.lineas().getFirst().elemento().equals(v), "Se alteró el orden de las líneas");
            }
            try {
                c.cambiarNivel(p.id(), Nivel.RESCATISTA);
                throw new AssertionError("No se detectó la actualización fallida");
            } catch (PersistenciaException esperada) {
                verificar(p.nivel() == Nivel.GENERAL && t.total() == 400,
                        "La escritura fallida cambió el descuento en memoria");
            }
        }
    }
}
