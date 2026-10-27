package mx.tecnm.orizaba.tp.veterinaria.app;

import com.formdev.flatlaf.FlatLightLaf;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;
import mx.tecnm.orizaba.tp.veterinaria.ui.VentanaClinica;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

/** Punto de entrada de la aplicación de escritorio. */
public final class Aplicacion {
    private Aplicacion() {}
    /** Abre datos.db o el archivo recibido como primer argumento. */
    public static void main(String[] args) {
        FlatLightLaf.setup();
        try {
            Clinica clinica = Clinica.abrirArchivo(Path.of(args.length == 0 ? "datos.db" : args[0]));
            SwingUtilities.invokeLater(() -> new VentanaClinica(clinica).setVisible(true));
        } catch (RuntimeException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,e.getMessage(),"No se pudo abrir la clínica",JOptionPane.ERROR_MESSAGE));
        }
    }
}
