package mx.tecnm.orizaba.tp.veterinaria.app;

import mx.tecnm.orizaba.tp.veterinaria.eventos.OyenteEvento;
import mx.tecnm.orizaba.tp.veterinaria.eventos.Evento;
import mx.tecnm.orizaba.tp.veterinaria.funcional.Reglas;
import mx.tecnm.orizaba.tp.veterinaria.modelo.*;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recorrido de consola por el modelo: sirve como referencia de uso para construir la UI.
 * La UI del estudiante debe ofrecer, como mínimo, lo que aquí se muestra.
 */
public final class DemoModelo {

    /** Ejecuta el programa con los argumentos indicados. */
    public static void main(String[] args) throws Exception {
        Path bd = args.length > 0 ? Path.of(args[0]) : Files.createTempFile("veterinaria", ".db");
        AtomicReference<LocalDate> hoy = new AtomicReference<>(LocalDate.now());

        try (ConexionSQLite cx = new ConexionSQLite(bd.toString());
             Clinica g = new Clinica(cx, hoy::get)) {

            // Suscripción con clase anónima (la UI usaría Platform.runLater / invokeLater aquí).
            g.suscribir(new OyenteEvento() {
                /** Recibe un evento del dominio en el hilo que lo publica. */
                @Override
                public void notificar(Evento e) {
                    System.out.println("   evento " + e + " [" + Thread.currentThread().getName() + "]");
                }
            });

            System.out.println("== Alta de procedimientos (referencias a constructor)");
            g.registrar(Procedimiento.Vacuna::new, "Vacuna antirrábica", 350.0, "Rabia");
            g.registrar(Procedimiento.Vacuna::new, "Vacuna múltiple canina", 480.0, "Séxtuple");
            g.registrar(Procedimiento.Consulta::new, "Consulta general", 400.0, 1);
            g.registrar(Procedimiento.Consulta::new, "Consulta de urgencia", 400.0, 3);
            g.registrar(Procedimiento.Cirugia::new, "Esterilización", 2500.0, 2);
            g.registrar(Procedimiento.Cirugia::new, "Cirugía ortopédica", 5000.0, 4);

            System.out.println("== propietarios");
            g.registrarPropietario("Fernanda Mora", "fer.mora@gmail.com", Nivel.GENERAL);
            g.registrarPropietario("Gustavo Lara", "glara@hotmail.com", Nivel.PLAN_SALUD);
            g.registrarPropietario("Refugio Patitas A.C.", "contacto@patitas.org", Nivel.RESCATISTA);

            System.out.println("== Flujo de atención");
            Atencion t = g.abrir(1);
            g.agregarLinea(t.id(), "VAC-0001", 2);
            g.agregarLinea(t.id(), "CIR-0001", 3);
            g.iniciar(t.id());
            t.forEach(System.out::println);
            g.cerrar(t.id());
            Atencion t2 = g.abrir(2);
            g.agregarLinea(t2.id(), "CON-0001", 1);
            g.iniciar(t2.id());
            g.abrir(3);

            System.out.println("== Reportes");
            System.out.println(g.catalogo().resumen());
            System.out.println("Ingresos por tipo: " + g.ingresosPorTipo());
            System.out.println("Estadísticas: " + g.estadisticas());
            System.out.println("Conteo por estado: " + g.conteoPorEstado());
            g.mejores(3).forEach(System.out::println);
            g.buscar(Reglas.conCapacidad(Programable.class)).stream().map(g::describir).forEach(System.out::println);

            System.out.println("== Monitor en segundo plano (reloj simulado +" + (Clinica.PLAZO_DIAS + 1) + " días)");
            g.iniciarMonitor(100);
            hoy.set(hoy.get().plusDays(Clinica.PLAZO_DIAS + 1));
            Thread.sleep(400);
            g.detenerMonitor();
            System.out.println("Base de datos: " + bd.toAbsolutePath());
        }
    }
}
