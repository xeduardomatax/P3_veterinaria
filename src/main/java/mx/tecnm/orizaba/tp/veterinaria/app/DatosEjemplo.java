package mx.tecnm.orizaba.tp.veterinaria.app;

import mx.tecnm.orizaba.tp.veterinaria.modelo.*;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;
import java.nio.file.Files;
import java.nio.file.Path;

/** Genera la base solicitada: seis procedimientos, tres propietarios y cuatro estados. */
public final class DatosEjemplo {
    private DatosEjemplo() {}
    /** Crea una base nueva sin sobrescribir archivos existentes. */
    public static void main(String[] args) throws Exception {
        Path ruta = Path.of(args.length == 0 ? "datos.db" : args[0]);
        if (Files.exists(ruta)) throw new IllegalArgumentException("La base ya existe: " + ruta);
        try (Clinica c = Clinica.abrirArchivo(ruta)) {
            var v1 = c.registrar(Procedimiento.Vacuna::new,"Antirrábica",350,"Rabia");
            var v2 = c.registrar(Procedimiento.Vacuna::new,"Múltiple canina",450,"Polivalente");
            var q1 = c.registrar(Procedimiento.Consulta::new,"Consulta general",400,1);
            var q2 = c.registrar(Procedimiento.Consulta::new,"Consulta de urgencia",400,3);
            var s1 = c.registrar(Procedimiento.Cirugia::new,"Esterilización",1800,2);
            c.registrar(Procedimiento.Cirugia::new,"Cirugía ortopédica",5000,3);
            var p1 = c.registrarPropietario("Ana López","ana@example.com",Nivel.GENERAL);
            var p2 = c.registrarPropietario("Luis Pérez","luis@example.com",Nivel.PLAN_SALUD);
            var p3 = c.registrarPropietario("Marta Ruiz","marta@example.com",Nivel.RESCATISTA);
            var a1 = c.abrir(p1.id()); c.agregarLinea(a1.id(),q1.clave().toString(),1);
            var a2 = c.abrir(p2.id()); c.agregarLinea(a2.id(),v1.clave().toString(),2); c.iniciar(a2.id());
            var a3 = c.abrir(p3.id()); c.agregarLinea(a3.id(),s1.clave().toString(),3); c.agregarLinea(a3.id(),v2.clave().toString(),1); c.iniciar(a3.id()); c.cerrar(a3.id());
            var a4 = c.abrir(p1.id()); c.agregarLinea(a4.id(),q2.clave().toString(),1); c.cancelar(a4.id());
            System.out.println("Base de ejemplo creada: " + ruta);
        }
    }
}
