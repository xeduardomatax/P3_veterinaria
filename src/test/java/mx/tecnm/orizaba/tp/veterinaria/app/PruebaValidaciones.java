package mx.tecnm.orizaba.tp.veterinaria.app;

import mx.tecnm.orizaba.tp.veterinaria.modelo.CodigoClinico;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Procedimiento;
import mx.tecnm.orizaba.tp.veterinaria.modelo.ReglaNegocioException;

/** Regresión adicional: cuatro dígitos y prefijos coherentes con el subtipo. */
public final class PruebaValidaciones {
    private PruebaValidaciones() { }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void rechazar(Runnable operacion) {
        try { operacion.run(); }
        catch (IllegalArgumentException | IllegalStateException | ReglaNegocioException esperada) { return; }
        throw new AssertionError("Se aceptó una clave clínica inválida");
    }

    /** Ejecuta las regresiones independientes de la base de datos. */
    public static void main(String[] args) {
        CodigoClinico maximo = new CodigoClinico("TST", 9999);
        verificar("TST-9999".equals(maximo.toString()), "Formato de cuatro dígitos");
        verificar(CodigoClinico.parse("TST-9999").equals(maximo), "Conversión bidireccional");
        rechazar(() -> new CodigoClinico("TST", 10000));
        rechazar(() -> CodigoClinico.parse("TST-10000"));
        CodigoClinico.registrar(maximo);
        rechazar(() -> CodigoClinico.siguiente("TST"));
        rechazar(() -> new Procedimiento.Vacuna(
                new CodigoClinico("CIR", 99), "Rabia", 100, "Rabia"));
        rechazar(() -> new Procedimiento.Consulta(
                new CodigoClinico("VAC", 99), "General", 100, 1));
        rechazar(() -> new Procedimiento.Cirugia(
                new CodigoClinico("CON", 99), "Esterilización", 100, 2));
        try {
            new Procedimiento.Vacuna(new CodigoClinico("VAC", 99), "Rabia", 100, "Rabia");
        } catch (ReglaNegocioException e) {
            throw new AssertionError("Se rechazó la clave del tipo correcto", e);
        }
        System.out.println("PruebaValidaciones: aprobada (formato, límite y prefijos)");
    }
}
