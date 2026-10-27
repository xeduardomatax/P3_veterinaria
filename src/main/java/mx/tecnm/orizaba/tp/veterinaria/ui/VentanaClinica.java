package mx.tecnm.orizaba.tp.veterinaria.ui;

import mx.tecnm.orizaba.tp.veterinaria.modelo.*;
import mx.tecnm.orizaba.tp.veterinaria.servicio.Clinica;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Ventana de escritorio. Los casos de uso se ejecutan fuera del EDT y sólo acceden a Clinica. */
public final class VentanaClinica extends JFrame {
    @java.io.Serial private static final long serialVersionUID = 1L;
    private final transient Clinica clinica;
    private final transient ExecutorService trabajos = Executors.newSingleThreadExecutor();
    private final JTable catalogo = tabla("Clave", "Tipo", "Nombre", "Precio base", "Atributo");
    private final JTable propietarios = tabla("ID", "Nombre", "Correo", "Nivel");
    private final JTable atenciones = tabla("ID", "Propietario", "Fecha", "Estado", "Subtotal", "Total");
    private final JTable lineas = tabla("Clave", "Procedimiento", "Sesiones", "Subtotal", "Importe");
    private final JTextArea eventos = new JTextArea();
    private final JTextArea reportes = new JTextArea();
    private final JLabel estado = new JLabel("Cargando…");
    private final JTextField filtro = new JTextField(22);
    private final JSpinner top = new JSpinner(new SpinnerNumberModel(3,1,100,1));
    private transient List<Procedimiento> elementos = List.of();
    private transient List<Propietario> actores = List.of();
    private transient List<Atencion> registros = List.of();
    private boolean cerrando;

    /** Construye las cinco pantallas mínimas y suscribe los eventos del monitor. */
    public VentanaClinica(Clinica clinica) {
        super("Clínica veterinaria · Control de atenciones");
        this.clinica = clinica;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1000,680)); setSize(1180,780); setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout(12,12)); raiz.setBorder(BorderFactory.createEmptyBorder(18,20,12,20));
        JLabel titulo = new JLabel("Control de atenciones"); titulo.setFont(titulo.getFont().deriveFont(Font.BOLD,25f));
        raiz.add(titulo,BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Procedimientos", panelCatalogo()); tabs.addTab("Propietarios",panelPropietarios());
        tabs.addTab("Atenciones",panelAtenciones()); tabs.addTab("Reportes",panelReportes());
        eventos.setEditable(false); eventos.setLineWrap(true); eventos.setWrapStyleWord(true);
        tabs.addTab("Bitácora de eventos",new JScrollPane(eventos));
        raiz.add(tabs,BorderLayout.CENTER); raiz.add(estado,BorderLayout.SOUTH); setContentPane(raiz);
        clinica.suscribir(e -> SwingUtilities.invokeLater(() -> {
            eventos.append(e.instante().withNano(0) + "  " + e + "\n");
            eventos.setCaretPosition(eventos.getDocument().getLength());
            if (!cerrando) ejecutar("Actualizando", () -> {});
        }));
        atenciones.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) mostrarLineas(); });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { cerrar(); }
        });
        ejecutar("Iniciando monitor", () -> clinica.iniciarMonitor(30_000));
    }

    private static JTable tabla(String... columnas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas,0) {
            @Override public boolean isCellEditable(int fila,int columna) { return false; }
        };
        JTable t = new JTable(modelo); t.setRowHeight(30); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true); t.setFillsViewportHeight(true); return t;
    }
    private static JPanel barra() { return new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); }
    private static void boton(JPanel p,String texto,Runnable accion) {
        JButton b = new JButton(texto); b.addActionListener(e -> accion.run()); p.add(b);
    }
    private JPanel panelCatalogo() {
        JPanel p = new JPanel(new BorderLayout()); JPanel b = barra();
        boton(b,"Registrar",() -> editarElemento(false)); boton(b,"Modificar",() -> editarElemento(true));
        boton(b,"Eliminar",() -> { String clave = claveSeleccionada(); if (clave != null && confirmar("Eliminar " + clave + "?")) ejecutar("Eliminando",() -> clinica.retirar(CodigoClinico.parse(clave))); });
        b.add(new JLabel("Nombre:")); b.add(filtro); boton(b,"Buscar",this::mostrarCatalogo);
        boton(b,"Respaldar",() -> archivo(false)); boton(b,"Restaurar catálogo",() -> archivo(true));
        p.add(b,BorderLayout.NORTH); p.add(new JScrollPane(catalogo)); return p;
    }
    private JPanel panelPropietarios() {
        JPanel p = new JPanel(new BorderLayout()); JPanel b = barra();
        boton(b,"Registrar",() -> editarPropietario(false)); boton(b,"Modificar",() -> editarPropietario(true));
        boton(b,"Eliminar",() -> { Integer id = seleccionado(propietarios); if (id != null && confirmar("Eliminar propietario #" + id + "?")) ejecutar("Eliminando",() -> clinica.eliminarPropietario(id)); });
        p.add(b,BorderLayout.NORTH); p.add(new JScrollPane(propietarios)); return p;
    }
    private JPanel panelAtenciones() {
        JPanel p = new JPanel(new BorderLayout()); JPanel b = barra();
        boton(b,"Abrir atención",this::abrirAtencion); boton(b,"Agregar línea",this::agregarLinea);
        boton(b,"Quitar línea",() -> { Integer id = seleccionado(atenciones); int fila = lineas.getSelectedRow(); if (id == null || fila < 0) { aviso("Seleccione una atención y una línea"); return; } String clave = lineas.getValueAt(fila,0).toString(); ejecutar("Quitando línea",() -> clinica.quitarLinea(id,clave)); });
        boton(b,"Iniciar",() -> transitar(0)); boton(b,"Concluir",() -> transitar(1)); boton(b,"Cancelar",() -> transitar(2));
        boton(b,"Eliminar",() -> { Integer id = seleccionado(atenciones); if (id != null && confirmar("Eliminar definitivamente la atención #" + id + " y sus líneas?")) ejecutar("Eliminando",() -> clinica.eliminarAtencion(id)); });
        JPanel detalle = new JPanel(new BorderLayout()); detalle.add(new JLabel("Procedimientos de la atención seleccionada"),BorderLayout.NORTH); detalle.add(new JScrollPane(lineas));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(atenciones),detalle); split.setResizeWeight(.6);
        p.add(b,BorderLayout.NORTH); p.add(split); return p;
    }
    private JPanel panelReportes() {
        JPanel p = new JPanel(new BorderLayout()); JPanel b = barra();
        b.add(new JLabel("N resultados:")); b.add(top); boton(b,"Actualizar reportes",() -> ejecutar("Generando reportes",() -> {}));
        reportes.setEditable(false); reportes.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
        p.add(b,BorderLayout.NORTH); p.add(new JScrollPane(reportes)); return p;
    }
    private String claveSeleccionada() {
        int fila = catalogo.getSelectedRow(); if (fila < 0) { aviso("Seleccione un procedimiento"); return null; }
        return catalogo.getValueAt(fila,0).toString();
    }
    private Integer seleccionado(JTable t) {
        int fila = t.getSelectedRow(); if (fila < 0) { aviso("Seleccione un registro"); return null; }
        return (Integer)t.getValueAt(fila,0);
    }
    private void editarElemento(boolean editar) {
        Procedimiento actual = null;
        if (editar) { String clave = claveSeleccionada(); if (clave == null) return; actual = elementos.stream().filter(e -> e.clave().toString().equals(clave)).findFirst().orElseThrow(); }
        JComboBox<String> tipo = new JComboBox<>(new String[]{"Vacuna","Consulta","Cirugia"});
        JTextField nombre = new JTextField(actual == null ? "" : actual.nombre(),24);
        JTextField precio = new JTextField(actual == null ? "" : Double.toString(actual.precioBase()));
        JTextField atributo = new JTextField(actual == null ? "" : atributo(actual));
        JLabel etiqueta = new JLabel();
        Runnable rotular = () -> etiqueta.setText(switch (tipo.getSelectedIndex()) { case 0 -> "Biológico"; case 1 -> "Prioridad (1 a 3)"; default -> "Horas de quirófano"; });
        tipo.addActionListener(e -> rotular.run());
        if (actual != null) { tipo.setSelectedItem(actual.tipo()); tipo.setEnabled(false); } rotular.run();
        if (!formulario(editar ? "Modificar procedimiento" : "Registrar procedimiento", "Tipo",tipo,"Nombre",nombre,"Precio base",precio,etiqueta,atributo)) return;
        String clave = actual == null ? null : actual.clave().toString(); int indice = tipo.getSelectedIndex();
        String n = nombre.getText(), pr = precio.getText(), a = atributo.getText();
        ejecutar("Guardando procedimiento",() -> {
            double monto = Double.parseDouble(pr);
            if (editar) clinica.editarProcedimiento(clave,n,monto,a);
            else switch (indice) {
                case 0 -> clinica.registrar(Procedimiento.Vacuna::new,n,monto,a);
                case 1 -> clinica.registrar(Procedimiento.Consulta::new,n,monto,Integer.parseInt(a));
                case 2 -> clinica.registrar(Procedimiento.Cirugia::new,n,monto,Integer.parseInt(a));
                default -> throw new IllegalArgumentException("Seleccione el tipo");
            }
        });
    }
    private static String atributo(Procedimiento p) {
        return switch (p) { case Procedimiento.Vacuna v -> v.biologico(); case Procedimiento.Consulta c -> Integer.toString(c.prioridad()); case Procedimiento.Cirugia c -> Integer.toString(c.horasQuirofano()); };
    }
    private void editarPropietario(boolean editar) {
        Propietario actual = null;
        if (editar) { Integer id = seleccionado(propietarios); if (id == null) return; actual = actores.stream().filter(a -> a.id() == id).findFirst().orElseThrow(); }
        JTextField nombre = new JTextField(actual == null ? "" : actual.nombre(),24);
        JTextField correo = new JTextField(actual == null ? "" : actual.correo()); JComboBox<Nivel> nivel = new JComboBox<>(Nivel.values());
        if (actual != null) nivel.setSelectedItem(actual.nivel());
        if (!formulario("Propietario","Nombre",nombre,"Correo",correo,"Nivel",nivel)) return;
        int id = actual == null ? 0 : actual.id(); String n = nombre.getText(), c = correo.getText(); Nivel v = (Nivel)nivel.getSelectedItem();
        ejecutar("Guardando propietario",() -> { if (editar) clinica.actualizarPropietario(id,n,c,v); else clinica.registrarPropietario(n,c,v); });
    }
    private void abrirAtencion() {
        if (actores.isEmpty()) { aviso("Registre primero un propietario"); return; }
        JComboBox<Propietario> propietario = new JComboBox<>(actores.toArray(Propietario[]::new));
        if (formulario("Abrir atención","Propietario",propietario)) { int id = ((Propietario)propietario.getSelectedItem()).id(); ejecutar("Abriendo atención",() -> clinica.abrir(id)); }
    }
    private void agregarLinea() {
        Integer id = seleccionado(atenciones); if (id == null) return;
        if (elementos.isEmpty()) { aviso("Registre primero un procedimiento"); return; }
        JComboBox<Procedimiento> procedimiento = new JComboBox<>(elementos.toArray(Procedimiento[]::new));
        JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1,1,100000,1));
        if (formulario("Agregar procedimiento","Procedimiento",procedimiento,"Sesiones",cantidad)) {
            String clave = ((Procedimiento)procedimiento.getSelectedItem()).clave().toString(); int unidades = (Integer)cantidad.getValue();
            ejecutar("Agregando línea",() -> clinica.agregarLinea(id,clave,unidades));
        }
    }
    private void transitar(int opcion) {
        Integer id = seleccionado(atenciones); if (id == null) return;
        if (opcion == 2 && !confirmar("Cancelar la atención #" + id + "?")) return;
        ejecutar("Actualizando estado",() -> { switch (opcion) { case 0 -> clinica.iniciar(id); case 1 -> clinica.cerrar(id); case 2 -> clinica.cancelar(id); default -> throw new IllegalArgumentException(); } });
    }
    private void archivo(boolean importar) {
        JFileChooser elegir = new JFileChooser(); elegir.setSelectedFile(new java.io.File("respaldo.bin"));
        int opcion = importar ? elegir.showOpenDialog(this) : elegir.showSaveDialog(this);
        if (opcion != JFileChooser.APPROVE_OPTION) return;
        Path ruta = elegir.getSelectedFile().toPath();
        if (!importar && java.nio.file.Files.exists(ruta) && !confirmar("Sobrescribir el respaldo seleccionado?")) return;
        if (importar && !confirmar("Restaurar los procedimientos del respaldo? Las claves coincidentes se actualizarán.")) return;
        ejecutar(importar ? "Restaurando catálogo" : "Exportando respaldo",() -> { if (importar) clinica.restaurarCatalogo(ruta); else clinica.exportarRespaldo(ruta); });
    }
    private boolean formulario(String titulo,Object... campos) {
        JPanel p = new JPanel(new GridLayout(0,2,10,12));
        for (int i=0;i<campos.length;i+=2) { p.add(campos[i] instanceof JLabel l ? l : new JLabel(campos[i].toString())); p.add((Component)campos[i+1]); }
        return JOptionPane.showConfirmDialog(this,p,titulo,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    private boolean confirmar(String texto) { return JOptionPane.showConfirmDialog(this,texto,"Confirmar",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    private void aviso(String texto) { JOptionPane.showMessageDialog(this,texto,"Clínica veterinaria",JOptionPane.INFORMATION_MESSAGE); }
    @FunctionalInterface private interface Trabajo { void ejecutar() throws Exception; }
    private record Vista(List<Procedimiento> elementos,List<Propietario> actores,List<Atencion> registros,String reporte) {}
    private void ejecutar(String mensaje,Trabajo trabajo) {
        if (cerrando) return;
        estado.setText(mensaje + "…"); int n = (Integer)top.getValue();
        trabajos.execute(new SwingWorker<Vista,Void>() {
            @Override protected Vista doInBackground() throws Exception {
                synchronized (clinica) {
                    trabajo.ejecutar();
                    return new Vista(clinica.catalogo().ordenados(Comparator.comparing(Procedimiento::clave)),clinica.propietarios(),clinica.registros(),reporte(n));
                }
            }
            @Override protected void done() {
                try {
                    Vista v = get(); elementos = v.elementos(); actores = v.actores(); registros = v.registros();
                    refrescar(); reportes.setText(v.reporte()); reportes.setCaretPosition(0); estado.setText("Listo · Monitor cada 30 segundos · Datos guardados en SQLite");
                } catch (Exception e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause();
                    String texto = causa instanceof NumberFormatException ? "Escriba un número válido en los campos numéricos." : causa.getMessage();
                    JOptionPane.showMessageDialog(VentanaClinica.this,texto == null ? "No se pudo completar la operación" : texto,"No se pudo completar",JOptionPane.WARNING_MESSAGE);
                    estado.setText("Operación no completada");
                }
            }
        });
    }
    private String reporte(int n) {
        StringBuilder s = new StringBuilder("INGRESOS POR TIPO · Atenciones concluidas\n\n");
        clinica.ingresosPorTipo().forEach((tipo,monto) -> s.append(String.format("%-15s $%,.2f%n",tipo,monto)));
        var particion = clinica.particionarAtenciones();
        s.append("\nATENCIONES\nConcluidas: ").append(particion.get(true).size()).append("\nNo concluidas: ").append(particion.get(false).size()).append('\n');
        var est = clinica.estadisticasTarifas();
        s.append(String.format("%nTARIFAS · Una sesión por procedimiento%nCantidad: %d%nMínimo: $%,.2f%nPromedio: $%,.2f%nMáximo: $%,.2f%n",est.getCount(),est.getCount()==0?0:est.getMin(),est.getAverage(),est.getCount()==0?0:est.getMax()));
        s.append("\nPROCEDIMIENTOS MÁS RENTABLES · Top ").append(n).append('\n');
        clinica.masRentables(n).forEach(e -> s.append(String.format("%s · %s: $%,.2f%n",e.getKey().clave(),e.getKey().nombre(),e.getValue())));
        return s.toString();
    }
    private void refrescar() {
        Integer id = atenciones.getSelectedRow() < 0 ? null : (Integer)atenciones.getValueAt(atenciones.getSelectedRow(),0);
        mostrarCatalogo(); DefaultTableModel mp = (DefaultTableModel)propietarios.getModel(); mp.setRowCount(0);
        actores.forEach(a -> mp.addRow(new Object[]{a.id(),a.nombre(),a.correo(),a.nivel()}));
        DefaultTableModel ma = (DefaultTableModel)atenciones.getModel(); ma.setRowCount(0);
        registros.forEach(t -> ma.addRow(new Object[]{t.id(),t.propietario().nombre(),t.fecha(),t.estado(),dinero(t.subtotal()),dinero(t.total())}));
        if (id != null) for (int i=0;i<atenciones.getRowCount();i++) if (id.equals(atenciones.getValueAt(i,0))) atenciones.setRowSelectionInterval(i,i);
        mostrarLineas();
    }
    private void mostrarCatalogo() {
        DefaultTableModel m = (DefaultTableModel)catalogo.getModel(); m.setRowCount(0);
        String texto = filtro.getText().toLowerCase(java.util.Locale.ROOT);
        elementos.stream().filter(p -> p.nombre().toLowerCase(java.util.Locale.ROOT).contains(texto)).forEach(p -> m.addRow(new Object[]{p.clave(),p.tipo(),p.nombre(),dinero(p.precioBase()),atributo(p)}));
    }
    private static String dinero(double monto) { return String.format("$%,.2f",monto); }
    private void mostrarLineas() {
        DefaultTableModel m = (DefaultTableModel)lineas.getModel(); m.setRowCount(0);
        int fila = atenciones.getSelectedRow(); if (fila < 0) return;
        int id = (Integer)atenciones.getValueAt(fila,0);
        registros.stream().filter(t -> t.id() == id).findFirst().ifPresent(t -> t.forEach(l -> m.addRow(new Object[]{l.elemento().clave(),l.elemento().nombre(),l.unidades(),dinero(l.subtotal()),dinero(l.importe())})));
    }
    private void cerrar() {
        if (cerrando) return; cerrando = true; estado.setText("Cerrando monitor y base de datos…");
        trabajos.execute(() -> { clinica.close(); SwingUtilities.invokeLater(this::dispose); }); trabajos.shutdown();
    }
}
