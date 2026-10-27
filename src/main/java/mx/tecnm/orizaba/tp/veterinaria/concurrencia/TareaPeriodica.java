package mx.tecnm.orizaba.tp.veterinaria.concurrencia;

/**
 * Tarea que se ejecuta periódicamente en su propio hilo.
 * <ul>
 *   <li>Implementa {@link Runnable} en lugar de extender {@link Thread}
 *       (véase {@code java_4_modelo/t6_concurrencia/EjemploHilos}).</li>
 *   <li>Detención cooperativa: bandera {@code volatile} + {@code interrupt()} + {@code join()}.</li>
 *   <li>Template Method: {@link #run()} es final; la variación está en {@link #ejecutarCiclo()}.</li>
 * </ul>
 */
public abstract class TareaPeriodica implements Runnable {

    private final long periodoMs;
    private volatile boolean activa;
    private Thread hilo;
    private volatile long ciclos;

    protected TareaPeriodica(long periodoMs) {
        if (periodoMs <= 0) {
            throw new IllegalArgumentException("Periodo inválido: " + periodoMs);
        }
        this.periodoMs = periodoMs;
    }

    /** Arranca el hilo (demonio) con el nombre indicado. */
    public final synchronized void iniciar(String nombreHilo) {
        if (activa || (hilo != null && hilo.isAlive())) {
            throw new IllegalStateException("El monitor está activo o todavía se está deteniendo");
        }
        hilo = new Thread(this, nombreHilo);
        hilo.setDaemon(true);
        activa = true;
        hilo.start();
    }

    /** Solicita la detención y espera a que el hilo termine. */
    public final void detener() throws InterruptedException {
        Thread actual;
        synchronized (this) { activa = false; actual = hilo; if (actual != null) actual.interrupt(); }
        if (actual != null && actual != Thread.currentThread()) actual.join();
    }

    /** Indica si el hilo periódico está activo. */
    public final boolean activa() {
        return activa;
    }

    /** Devuelve el número de ciclos completados. */
    public final long ciclos() {
        return ciclos;
    }

    /** Ejecuta ciclos periódicos hasta la detención o interrupción del hilo. */
    @Override
    public final void run() {
        try {
            while (activa) { ejecutarCiclo(); ciclos++; Thread.sleep(periodoMs); }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally { activa = false; }
    }

    /** Trabajo de un ciclo. */
    protected abstract void ejecutarCiclo();
}
