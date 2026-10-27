package mx.tecnm.orizaba.tp.veterinaria.app;

import com.formdev.flatlaf.FlatLightLaf;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;
import mx.tecnm.orizaba.tp.veterinaria.ui.VentanaClinica;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

/** Revisa la carga de las cinco pantallas y captura su representación real Swing. */
public final class PruebaEscritorio {
    private PruebaEscritorio() {}
    private static <T extends Component> T encontrar(Container c,Class<T> tipo) {
        for (Component hijo : c.getComponents()) {
            if (tipo.isInstance(hijo)) return tipo.cast(hijo);
            if (hijo instanceof Container contenedor) { T encontrado = encontrar(contenedor,tipo); if (encontrado != null) return encontrado; }
        }
        return null;
    }
    /** Usa una copia temporal de datos.db; no altera los datos de entrega. */
    public static void main(String[] args) throws Exception {
        FlatLightLaf.setup();
        Path copia = Files.createTempDirectory("ui-clinica").resolve("datos.db"); Files.copy(Path.of("datos.db"),copia);
        Clinica c = Clinica.abrirArchivo(copia);
        AtomicReference<VentanaClinica> referencia = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { var ventana = new VentanaClinica(c); referencia.set(ventana); ventana.setVisible(true); });
        VentanaClinica ventana = referencia.get();
        try {
            boolean listo = false;
            for (int i=0;i<100;i++) {
                AtomicReference<Boolean> cargado = new AtomicReference<>(false);
                SwingUtilities.invokeAndWait(() -> { JTable tabla = encontrar(ventana,JTable.class); cargado.set(tabla != null && tabla.getRowCount() == 6); });
                if (cargado.get()) { listo = true; break; }
                Thread.sleep(100);
            }
            if (!listo) throw new AssertionError("La UI no cargó los seis procedimientos");
            String[] nombres = {"procedimientos","propietarios","atenciones","reportes","eventos"};
            for (int i=0;i<5;i++) {
                if (i == 4) {
                    var atencion = c.abrir(c.propietarios().getFirst().id());
                    c.agregarLinea(atencion.id(),c.catalogo().iterator().next().clave().toString(),1);
                    c.iniciar(atencion.id()); c.cerrar(atencion.id());
                }
                final int indice = i;
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        JTabbedPane tabs = encontrar(ventana,JTabbedPane.class); tabs.setSelectedIndex(indice);
                        Component pantalla = tabs.getSelectedComponent();
                        if (indice < 3) {
                            JTable tabla = encontrar((Container)pantalla,JTable.class);
                            int esperado = new int[]{6,3,4}[indice];
                            if (tabla.getRowCount() != esperado) throw new AssertionError("Cantidad incorrecta en pantalla " + indice);
                            if (indice == 2) tabla.setRowSelectionInterval(2,2);
                        }
                        ventana.validate();
                        BufferedImage imagen = new BufferedImage(ventana.getContentPane().getWidth(),ventana.getContentPane().getHeight(),BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = imagen.createGraphics(); ventana.getContentPane().printAll(g); g.dispose();
                        ImageIO.write(imagen,"png",Path.of("docs/capturas/" + nombres[indice] + ".png").toFile());
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
            System.out.println("Escritorio: cinco pantallas verificadas, listados 6/3/4 y capturas generadas");
        } finally { SwingUtilities.invokeAndWait(() -> ventana.dispatchEvent(new WindowEvent(ventana,WindowEvent.WINDOW_CLOSING))); }
    }
}
