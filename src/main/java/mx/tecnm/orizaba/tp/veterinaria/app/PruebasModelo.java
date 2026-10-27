package mx.tecnm.orizaba.tp.veterinaria.app;

import mx.tecnm.orizaba.tp.veterinaria.eventos.Evento;
import mx.tecnm.orizaba.tp.veterinaria.eventos.TipoEvento;
import mx.tecnm.orizaba.tp.veterinaria.funcional.FabricaElemento;
import mx.tecnm.orizaba.tp.veterinaria.funcional.Regla;
import mx.tecnm.orizaba.tp.veterinaria.funcional.Reglas;
import mx.tecnm.orizaba.tp.veterinaria.modelo.*;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.Respaldo;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pruebas de aceptación del modelo (sin dependencias externas). Deben pasar al 100 %
 * antes de comenzar la UI. Ejecutar: {@code java ... mx.tecnm.orizaba.tp.veterinaria.app.PruebasModelo}
 */
public final class PruebasModelo {

    private static int aprobadas;
    private static final List<String> fallidas = new ArrayList<>();

    @FunctionalInterface
    private interface Caso {
        void ejecutar() throws Exception;
    }

    private static void prueba(String nombre, Caso caso) {
        try {
            caso.ejecutar();
            aprobadas++;
            System.out.println("  [OK]    " + nombre);
        } catch (Throwable t) {
            fallidas.add(nombre);
            System.out.println("  [FALLA] " + nombre + " -> " + t);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    private static void cerca(double esperado, double real) {
        verificar(Math.abs(esperado - real) < 0.01, "se esperaba " + esperado + " y se obtuvo " + real);
    }

    private static void lanza(Class<? extends Throwable> tipo, Caso caso) {
        try {
            caso.ejecutar();
        } catch (Throwable t) {
            verificar(tipo.isInstance(t), "se esperaba " + tipo.getSimpleName() + " y se obtuvo " + t);
            return;
        }
        throw new AssertionError("se esperaba " + tipo.getSimpleName());
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("pruebas-veterinaria");
        System.out.println("Pruebas del modelo – Clinica");

        prueba("T01 CodigoClinico: formato, parse y validación", () -> {
            CodigoClinico c = CodigoClinico.parse("ABC-0042");
            verificar(c.toString().equals("ABC-0042"), "toString");
            verificar(c.compareTo(new CodigoClinico("ABC", 43)) < 0, "orden");
            lanza(IllegalArgumentException.class, () -> new CodigoClinico("ab", 1));
            lanza(IllegalArgumentException.class, () -> CodigoClinico.parse("ABC_1"));
        });

        prueba("T02 Invariantes de Procedimiento (encapsulamiento)", () -> {
            lanza(ReglaNegocioException.class, () -> new Procedimiento.Vacuna("x", -1, "Rabia"));
            lanza(ReglaNegocioException.class, () -> new Procedimiento.Vacuna(" ", 10, "Rabia"));
            lanza(ReglaNegocioException.class, () -> new Procedimiento.Consulta("x", 10, 0));
            lanza(ReglaNegocioException.class, () -> new Procedimiento.Vacuna("x", 10, "Rabia").tarifa(0));
        });

        prueba("T03 Polimorfismo: tarifas por subtipo (Template Method)", () -> {
            Facturable[] v = {
                new Procedimiento.Vacuna("a", 350.0, "Rabia"),
                new Procedimiento.Consulta("b", 400.0, 3),
                new Procedimiento.Cirugia("c", 5000.0, 3) };
            cerca(700.0, v[0].tarifa(2));
            cerca(600.0, v[1].tarifa(1));
            cerca(16000.0, v[2].tarifa(4));
            cerca(Math.round(700.0 * 1.16 * 100.0) / 100.0, v[0].tarifaConImpuesto(2));
        });

        prueba("T04 Clasificación múltiple y colisión de métodos default", () -> {
            Object s1 = new Procedimiento.Vacuna("a", 1, "Rabia");
            Object s2 = new Procedimiento.Consulta("b", 1, 3);
            Procedimiento.Cirugia s3 = new Procedimiento.Cirugia("c", 1, 3);
            verificar(s1 instanceof Programable && !(s1 instanceof Supervisado), "Vacuna sólo Programable");
            verificar(s2 instanceof Supervisado && !(s2 instanceof Programable), "Consulta sólo Supervisado");
            verificar(s3 instanceof Programable && s3 instanceof Supervisado, "Cirugia ambas");
            String d = s3.descripcionCorta();
            verificar(d.contains("Refuerzo cada (días)") && d.contains("Responsable"), "colisión resuelta: " + d);
        });

        prueba("T05 Retorno covariante en copiar()", () -> {
            Procedimiento.Vacuna a = new Procedimiento.Vacuna("a", 5, "Rabia");
            Procedimiento.Vacuna b = a.copiar();   // sin conversión: retorno covariante
            verificar(!a.equals(b) && a.nombre().equals(b.nombre()), "copia con clave nueva");
        });

        prueba("T06 Máquina de estados de Atencion", () -> {
            Propietario x = new Propietario(900, "Prueba", "prueba@ito.mx");
            Atencion t = new Atencion(900, x, LocalDate.now());
            lanza(ReglaNegocioException.class, t::iniciar);
            t.agregar(new Procedimiento.Vacuna("a", 10, "Rabia"), 1);
            t.iniciar();
            lanza(ReglaNegocioException.class, () -> t.agregar(new Procedimiento.Vacuna("b", 1, "Rabia"), 1));
            t.cerrar();
            lanza(ReglaNegocioException.class, t::cancelar);
            verificar(t.estado() == Estado.CERRADA, "estado final");
        });

        prueba("T07 Composición: la línea usa el nivel de la envolvente", () -> {
            Nivel n = Nivel.values()[Nivel.values().length - 1];
            Propietario x = new Propietario(901, "Prueba", "p2@ito.mx", n);
            Atencion t = new Atencion(901, x, LocalDate.now());
            Atencion.Linea l = t.agregar(new Procedimiento.Vacuna("a", 350.0, "Rabia"), 2);
            cerca(n.aplicar(700.0), l.importe());
            verificar(l.contenedor() == t, "Atencion.this");
            lanza(ReglaNegocioException.class, () -> t.agregar(l.elemento(), 2));
        });

        prueba("T08 Agregación, iterador oculto, clase anónima y clase local", () -> {
            Procedimiento a = new Procedimiento.Vacuna("Alfa", 300, "Rabia");
            Procedimiento b = new Procedimiento.Consulta("Beta", 100, 3);
            CatalogoProcedimientos cat = new CatalogoProcedimientos(a, b);
            lanza(ReglaNegocioException.class, () -> cat.agregar(a));
            Iterator<Procedimiento> it = cat.iterator();
            verificar(!it.getClass().getSimpleName().isEmpty() && it.hasNext(), "iterador");
            lanza(UnsupportedOperationException.class, it::remove);
            verificar(cat.ordenados(CatalogoProcedimientos.porTarifa(1)).get(0) == b, "comparador anónimo");
            verificar(cat.resumen().startsWith("2 "), "clase local: " + cat.resumen());
            verificar(cat.deTipo(Procedimiento.Vacuna.class).size() == 1, "genéricos acotados");
        });

        prueba("T09 Interfaz funcional propia y composición de reglas", () -> {
            Procedimiento a = new Procedimiento.Vacuna("Alfa", 300, "Rabia");
            Procedimiento b = new Procedimiento.Cirugia("Gamma", 100, 3);
            CatalogoProcedimientos cat = new CatalogoProcedimientos(a, b);
            Regla<Procedimiento> r = Reglas.precioHasta(200).y(Reglas.conCapacidad(Supervisado.class));
            verificar(cat.filtrar(r).equals(List.of(b)), "y");
            verificar(cat.filtrar(r.negar()).equals(List.of(a)), "negar");
            verificar(cat.filtrar(Reglas.nombreContiene("ALF").o(r)).size() == 2, "o");
            FabricaElemento<Procedimiento.Vacuna, String> f = Procedimiento.Vacuna::new;
            verificar(f.crear("Delta", 1, "Rabia").tipo().equals("Vacuna"), "referencia a constructor");
        });

        prueba("T10 Serialización: transient y Externalizable", () -> {
            Procedimiento a = new Procedimiento.Vacuna("Alfa", 10, "Rabia");
            a.tarifa(1);
            Propietario x = new Propietario(7, "Ada", "ada@ito.mx", Nivel.values()[1]);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bytes)) {
                oos.writeObject(a);
                oos.writeObject(x);
            }
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                Procedimiento a2 = (Procedimiento) ois.readObject();
                Propietario x2 = (Propietario) ois.readObject();
                verificar(a2.equals(a) && a2.cotizaciones() == 0 && a.cotizaciones() == 1, "transient");
                verificar(x2.correo().equals("ada@ito.mx") && x2.nivel() == Nivel.values()[1], "externalizable");
            }
        });

        Path bd = tmp.resolve("pruebas.db");
        AtomicReference<LocalDate> hoy = new AtomicReference<>(LocalDate.now());
        int[] ids = new int[2];

        prueba("T11 Servicio + SQLite: alta, flujo y reportes", () -> {
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Clinica g = new Clinica(cx, hoy::get);
                List<Evento> eventos = new CopyOnWriteArrayList<>();
                g.suscribir(eventos::add);
                Procedimiento e1 = g.registrar(Procedimiento.Vacuna::new, "Uno", 350.0, "Rabia");
                Procedimiento e3 = g.registrar(Procedimiento.Cirugia::new, "Tres", 5000.0, 3);
                Propietario a = g.registrarPropietario("Grace Hopper", "grace@ito.mx", Nivel.values()[0]);
                lanza(ReglaNegocioException.class, () -> g.registrarPropietario("Otra", "GRACE@ito.mx", Nivel.values()[0]));
                Atencion t = g.abrir(a.id());
                g.agregarLinea(t.id(), e1.clave().toString(), 2);
                g.agregarLinea(t.id(), e3.clave().toString(), 4);
                g.iniciar(t.id());
                g.cerrar(t.id());
                ids[0] = t.id();
                cerca(700.0 + 16000.0, g.estadisticas().getSum());
                verificar(g.ingresosPorTipo().size() == 2, "ingresos por tipo");
                verificar(g.mejores(1).equals(List.of(a)), "mejores");
                verificar(g.masDemandado().isPresent(), "más demandado");
                verificar(g.describir(e1).contains("Vacuna"), "describir");
                verificar(g.proyectar(Procedimiento::nombre).containsAll(List.of("Uno", "Tres")), "proyectar");
                verificar(eventos.stream().anyMatch(ev -> ev.tipo() == TipoEvento.ALTA), "eventos");
                lanza(ReglaNegocioException.class, () -> g.retirar(e1.clave()));
                ids[1] = g.abrir(a.id()).id();
            }
        });

        prueba("T12 Persistencia: la información sobrevive al reinicio", () -> {
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Clinica g = new Clinica(cx, hoy::get);
                verificar(g.catalogo().tamanio() == 2, "catálogo");
                verificar(g.propietarios().size() == 1, "propietarios");
                Atencion t = g.registro(ids[0]);
                verificar(t.estado() == Estado.CERRADA && t.lineas().size() == 2, "registro reconstruido");
                cerca(700.0 + 16000.0, t.total());
            }
        });

        prueba("T13 Monitor: reglas, hilo y detención", () -> {
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Clinica g = new Clinica(cx, hoy::get);
                List<Evento> eventos = new CopyOnWriteArrayList<>();
                g.suscribir(eventos::add);
                Atencion t = g.abrir(g.propietarios().get(0).id());
                g.agregarLinea(t.id(), g.catalogo().iterator().next().clave().toString(), 1);
                g.iniciar(t.id());
                hoy.set(hoy.get().plusDays(Clinica.PLAZO_DIAS + 1));
                verificar(g.revisar() == 2, "cancelación + alerta");
                verificar(g.registro(ids[1]).estado() == Estado.CANCELADA, "abierta sin líneas cancelada");
                verificar(g.revisar() == 0, "la alerta no se repite");
                g.iniciarMonitor(20);
                Thread.sleep(120);
                verificar(g.monitorActivo(), "hilo activo");
                g.detenerMonitor();
                verificar(!g.monitorActivo(), "hilo detenido");
                long alertas = eventos.stream().filter(ev -> ev.tipo() == TipoEvento.ALERTA).count();
                verificar(alertas == 1, "alertas: " + alertas);
            }
        });

        prueba("T14 Respaldo binario (exportar/importar)", () -> {
            Path archivo = tmp.resolve("respaldo.bin");
            try (ConexionSQLite cx = new ConexionSQLite(bd.toString())) {
                Clinica g = new Clinica(cx, hoy::get);
                g.exportarRespaldo(archivo);
                Respaldo.Contenido c = Respaldo.importar(archivo);
                verificar(c.catalogo().tamanio() == g.catalogo().tamanio(), "catálogo");
                verificar(c.registros().size() == g.registros().size(), "registros");
                Atencion.Linea l = c.registros().get(0).lineas().get(0);
                verificar(l.contenedor() == c.registros().get(0), "clase interna serializada con su envolvente");
            }
        });

        System.out.printf("%nResultado: %d aprobadas, %d fallidas%n", aprobadas, fallidas.size());
        if (!fallidas.isEmpty()) {
            System.exit(1);
        }
    }
}
