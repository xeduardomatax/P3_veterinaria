package mx.tecnm.orizaba.tp.veterinaria.app;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import mx.tecnm.orizaba.tp.veterinaria.concurrencia.TareaPeriodica;

/** Regresión: no crear dos hilos sobre la misma tarea durante una detención. */
public final class PruebaReinicioTarea {
    private PruebaReinicioTarea() { }

    /** Coordina el escenario con señales, sin depender de pausas arbitrarias. */
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch detencionSolicitada = new CountDownLatch(1);
        CountDownLatch terminarCiclo = new CountDownLatch(1);
        CountDownLatch cicloTerminado = new CountDownLatch(1);
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        TareaPeriodica tarea = new TareaPeriodica(10) {
            @Override
            protected void ejecutarCiclo() {
                try {
                    detener(); // El hilo puede pedir su propia detención sin hacerse join.
                    Thread.interrupted(); // Permite mantener abierto el ciclo de prueba.
                    detencionSolicitada.countDown();
                    if (!terminarCiclo.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("No se liberó el ciclo");
                    }
                } catch (Throwable e) {
                    fallo.set(e);
                } finally {
                    cicloTerminado.countDown();
                }
            }
        };
        tarea.iniciar("Prueba de detención");
        try {
            if (!detencionSolicitada.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("El hilo no solicitó detenerse");
            }
            try {
                tarea.iniciar("Reinicio prematuro");
                throw new AssertionError("Se permitió reiniciar con el hilo anterior vivo");
            } catch (IllegalStateException esperada) {
                // La bandera ya es falsa, pero el hilo anterior aún ejecuta el ciclo.
            }
        } finally {
            terminarCiclo.countDown();
            cicloTerminado.await(5, TimeUnit.SECONDS);
            tarea.detener();
        }
        if (fallo.get() != null) throw new AssertionError("Falló el ciclo", fallo.get());
        if (tarea.activa()) throw new AssertionError("La tarea quedó activa");
        tarea.iniciar("Reinicio después de join");
        tarea.detener();
        System.out.println("Concurrencia: reinicio durante detención rechazado; reinicio tras join permitido");
    }
}
