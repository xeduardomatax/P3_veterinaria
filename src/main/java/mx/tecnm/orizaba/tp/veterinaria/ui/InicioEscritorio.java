package mx.tecnm.orizaba.tp.veterinaria.ui;

import com.formdev.flatlaf.FlatLightLaf;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

/** Inicializa Swing y abre una única fachada; todo el código gráfico queda en ui. */
public final class InicioEscritorio {
    private InicioEscritorio() {}

    /** Abre datos.db o la ruta del primer argumento sin cargar SQLite en el EDT. */
    public static void main(String[] args) {
        Path archivo = Path.of(args.length == 0 ? "datos.db" : args[0]);
        Clinica clinica;
        try {
            clinica = Clinica.abrirArchivo(archivo);
        } catch (RuntimeException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    e.getMessage(), "No se pudo abrir la clínica", JOptionPane.ERROR_MESSAGE));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                new VentanaClinica(clinica).setVisible(true);
            } catch (RuntimeException e) {
                Thread cierre = new Thread(clinica::close, "Cierre por fallo de interfaz");
                cierre.start();
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        "No se pudo abrir la ventana", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
