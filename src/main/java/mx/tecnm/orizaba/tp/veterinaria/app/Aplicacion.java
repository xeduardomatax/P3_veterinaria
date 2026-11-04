package mx.tecnm.orizaba.tp.veterinaria.app;

import mx.tecnm.orizaba.tp.veterinaria.ui.InicioEscritorio;

/** Punto de entrada de la aplicación de escritorio. */
public final class Aplicacion {
    private Aplicacion() {}
    /** Abre datos.db o el archivo recibido como primer argumento. */
    public static void main(String[] args) {
        InicioEscritorio.main(args);
    }
}
