package mx.tecnm.orizaba.tp.veterinaria.servicio;

import mx.tecnm.orizaba.tp.veterinaria.concurrencia.TareaPeriodica;
import mx.tecnm.orizaba.tp.veterinaria.eventos.Evento;
import mx.tecnm.orizaba.tp.veterinaria.eventos.OyenteEvento;
import mx.tecnm.orizaba.tp.veterinaria.eventos.TipoEvento;
import mx.tecnm.orizaba.tp.veterinaria.funcional.FabricaElemento;
import mx.tecnm.orizaba.tp.veterinaria.funcional.Regla;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Propietario;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Procedimiento;
import mx.tecnm.orizaba.tp.veterinaria.modelo.CatalogoProcedimientos;
import mx.tecnm.orizaba.tp.veterinaria.modelo.CodigoClinico;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Estado;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Nivel;
import mx.tecnm.orizaba.tp.veterinaria.modelo.ReglaNegocioException;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Atencion;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.ConexionSQLite;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.Repositorio;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.RepositorioPropietarioSQLite;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.RepositorioProcedimientoSQLite;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.RepositorioAtencionSQLite;
import mx.tecnm.orizaba.tp.veterinaria.persistencia.Respaldo;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fachada del dominio (capa de servicio). <strong>La UI sólo debe depender de esta clase</strong>
 * y de los tipos del paquete {@code modelo}/{@code eventos}; nunca de JDBC.
 * <ul>
 *   <li>Concurrencia: {@link MonitorSeguimiento} es una clase interna que <em>es una</em>
 *       {@link TareaPeriodica} y <em>tiene acceso</em> a {@code Clinica.this}: herencia múltiple de
 *       implementación mediante clases internas (véase {@code J04_Multiple_De_Implementacion}).</li>
 *   <li>Programación funcional: reportes con streams y funciones de orden superior.</li>
 * </ul>
 */
public class Clinica implements AutoCloseable {
    private ConexionSQLite conexionPropia;

    /** Abre una clínica de escritorio; la fachada administra la conexión. */
    public static Clinica abrirArchivo(Path archivo) {
        ConexionSQLite bd = new ConexionSQLite(archivo.toString());
        try {
            Clinica clinica = new Clinica(bd);
            clinica.conexionPropia = bd;
            return clinica;
        } catch (RuntimeException e) { bd.close(); throw e; }
    }

    /** Modifica todos los datos editables de un procedimiento conservando su clave y tipo. */
    public synchronized void editarProcedimiento(String clave, String nombre, double precio, String atributo) {
        Procedimiento anterior = elemento(clave);
        Procedimiento nuevo = switch (anterior) {
            case Procedimiento.Vacuna v -> new Procedimiento.Vacuna(v.clave(),nombre,precio,atributo);
            case Procedimiento.Consulta c -> new Procedimiento.Consulta(c.clave(),nombre,precio,Integer.parseInt(atributo));
            case Procedimiento.Cirugia c -> new Procedimiento.Cirugia(c.clave(),nombre,precio,Integer.parseInt(atributo));
        };
        Map<Integer, Atencion> copias = reenlazar(List.of(nuevo));
        repoElementos.guardar(nuevo);
        aplicarCatalogo(List.of(nuevo), copias);
    }

    private Map<Integer, Atencion> reenlazar(List<Procedimiento> nuevos) {
        Map<CodigoClinico, Procedimiento> reemplazos = nuevos.stream()
                .collect(Collectors.toMap(Procedimiento::clave, Function.identity()));
        Map<Integer, Atencion> copias = new TreeMap<>();
        registros().forEach(t -> {
            Atencion copia = new Atencion(t.id(),t.propietario(),t.fecha());
            t.forEach(l -> copia.agregar(reemplazos.getOrDefault(l.elemento().clave(), l.elemento()),l.unidades()));
            copia.restaurarEstado(t.estado()); copias.put(copia.id(),copia);
        });
        return copias;
    }

    private void aplicarCatalogo(List<Procedimiento> nuevos, Map<Integer, Atencion> copias) {
        nuevos.forEach(p -> { catalogo.retirar(p.clave()); catalogo.agregar(p); });
        registros.putAll(copias);
    }

    /** Modifica un propietario después de validar el conjunto completo de datos. */
    public synchronized void actualizarPropietario(int id, String nombre, String correo, Nivel nivel) {
        Propietario validado = new Propietario(id,nombre,correo,nivel);
        if (propietarios.values().stream().anyMatch(p -> p.id() != id && p.correo().equals(validado.correo())))
            throw new ReglaNegocioException("El correo ya está registrado");
        Propietario actual = propietario(id);
        repoPropietarios.guardar(validado);
        actual.setNombre(validado.nombre()); actual.setCorreo(validado.correo()); actual.setNivel(nivel);
    }

    /** Elimina propietarios sin atenciones asociadas. */
    public synchronized boolean eliminarPropietario(int id) {
        if (registros.values().stream().anyMatch(t -> t.propietario().id() == id))
            throw new ReglaNegocioException("El propietario tiene atenciones asociadas");
        boolean eliminado = repoPropietarios.eliminar(id);
        if (eliminado) propietarios.remove(id);
        return eliminado;
    }

    /** Elimina una atención y sus líneas. */
    public synchronized boolean eliminarAtencion(int id) {
        boolean eliminado = repoRegistros.eliminar(id);
        if (eliminado) { registros.remove(id); alertados.remove(id); }
        return eliminado;
    }

    /** Particiona las atenciones en concluidas y no concluidas (RF07). */
    public Map<Boolean,List<Atencion>> particionarAtenciones() {
        return registros().stream().collect(Collectors.partitioningBy(t -> t.estado() == Estado.CERRADA));
    }

    /** Obtiene los N procedimientos con más ingresos en atenciones concluidas. */
    public List<Map.Entry<Procedimiento,Double>> masRentables(int n) {
        if (n < 0) throw new IllegalArgumentException("N debe ser no negativo");
        return cerrados().flatMap(t -> t.lineas().stream())
                .collect(Collectors.groupingBy(Atencion.Linea::elemento,Collectors.summingDouble(Atencion.Linea::importe)))
                .entrySet().stream().sorted(Map.Entry.<Procedimiento,Double>comparingByValue().reversed()
                    .thenComparing(e -> e.getKey().clave())).limit(n).toList();
    }

    /** Estadísticas de tarifa para una sesión de cada procedimiento. */
    public DoubleSummaryStatistics estadisticasTarifas() {
        return catalogo.ordenados(Comparator.comparing(Procedimiento::clave)).stream()
                .mapToDouble(p -> p.tarifa(1)).summaryStatistics();
    }

    /** Restaura el catálogo binario sin borrar propietarios ni atenciones existentes. */
    public synchronized void restaurarCatalogo(Path archivo) throws IOException, ClassNotFoundException {
        Respaldo.Contenido contenido = Respaldo.importar(archivo);
        List<Procedimiento> nuevos = contenido.catalogo().ordenados(Comparator.comparing(Procedimiento::clave));
        for (Procedimiento p : nuevos) {
            if (catalogo.buscar(p.clave()).isPresent()) {
                Procedimiento anterior = elemento(p.clave().toString());
                if (!anterior.tipo().equals(p.tipo())) throw new ReglaNegocioException("El respaldo cambia el tipo de " + p.clave());
            }
        }
        Map<Integer, Atencion> copias = reenlazar(nuevos);
        repoElementos.guardarTodos(nuevos);
        aplicarCatalogo(nuevos, copias);
        publicar(TipoEvento.ALTA,"Catálogo restaurado desde respaldo");
    }

    /** Días tras los cuales un(a) atención en curso genera una alerta. */
    public static final int PLAZO_DIAS = 3;

    private final CatalogoProcedimientos catalogo = new CatalogoProcedimientos();
    private final Map<Integer, Propietario> propietarios = new ConcurrentHashMap<>();
    private final Map<Integer, Atencion> registros = new ConcurrentHashMap<>();
    private final Set<Integer> alertados = ConcurrentHashMap.newKeySet();
    private final List<OyenteEvento> oyentes = new CopyOnWriteArrayList<>();

    private final RepositorioProcedimientoSQLite repoElementos;
    private final Repositorio<Propietario, Integer> repoPropietarios;
    private final Repositorio<Atencion, Integer> repoRegistros;
    private final Supplier<LocalDate> reloj;
    private MonitorSeguimiento monitor;

    /** Construye una instancia con los datos recibidos y valida sus invariantes. */
    public Clinica(ConexionSQLite bd) {
        this(bd, LocalDate::now);
    }

    /** @param reloj proveedor de la fecha actual (inyectable para pruebas y simulación) */
    public Clinica(ConexionSQLite bd, Supplier<LocalDate> reloj) {
        repoElementos = new RepositorioProcedimientoSQLite(bd);
        repoPropietarios = new RepositorioPropietarioSQLite(bd);
        repoRegistros = new RepositorioAtencionSQLite(bd,repoPropietarios,repoElementos);
        this.reloj = java.util.Objects.requireNonNull(reloj);
        cargar();
    }

    private void cargar() {
        repoElementos.todos().forEach(catalogo::agregar);
        repoPropietarios.todos().forEach(p -> propietarios.put(p.id(),p));
        repoRegistros.todos().forEach(t -> {
            Atencion copia = new Atencion(t.id(),propietarios.get(t.propietario().id()),t.fecha());
            t.forEach(l -> copia.agregar(catalogo.buscar(l.elemento().clave()).orElseThrow(),l.unidades()));
            copia.restaurarEstado(t.estado());
            registros.put(copia.id(),copia);
        });
    }

    // ------------------------------------------------------------------ eventos

    /** Agrega un oyente de los eventos del servicio. */
    public void suscribir(OyenteEvento oyente) {
        oyentes.add(oyente);
    }

    /** Retira un oyente de eventos. */
    public void desuscribir(OyenteEvento oyente) {
        oyentes.remove(oyente);
    }

    private void publicar(TipoEvento tipo, String mensaje) {
        Evento e = Evento.de(tipo, mensaje);
        oyentes.forEach(o -> o.notificar(e));
    }

    // ------------------------------------------------------------------ catálogo

    /**
     * Alta genérica de cualquier subtipo mediante una fábrica (función de orden superior).
     * Ejemplo: {@code registrar(Procedimiento.Vacuna::new, "...", 100.0, "Rabia")}.
     */
    public synchronized <E extends Procedimiento, A> E registrar(FabricaElemento<E, A> fabrica, String nombre, double precio, A atributo) {
        E e = fabrica.crear(nombre,precio,atributo);
        repoElementos.guardar(e); catalogo.agregar(e);
        publicar(TipoEvento.ALTA,"Procedimiento registrado: " + e.nombre());
        return e;
    }

    /** Valida y persiste el nombre y precio de un procedimiento. */
    public synchronized void actualizar(Procedimiento elemento, String nombre, double precio) {
        // Valida ambos valores antes de modificar el objeto compartido.
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre es obligatorio");
        if (!Double.isFinite(precio) || precio < 0) throw new ReglaNegocioException("Precio inválido");
        String anterior = elemento.nombre(); double precioAnterior = elemento.precioBase();
        elemento.setNombre(nombre);
        elemento.setPrecioBase(precio);
        try { repoElementos.guardar(elemento); }
        catch (RuntimeException e) { elemento.setNombre(anterior); elemento.setPrecioBase(precioAnterior); throw e; }
    }

    /** Retira un procedimiento; en la fachada impide retirar los utilizados. */
    public synchronized boolean retirar(CodigoClinico clave) {
        if (registros.values().stream().flatMap(t -> t.lineas().stream()).anyMatch(l -> l.elemento().clave().equals(clave))) throw new ReglaNegocioException("El procedimiento está utilizado en una atención");
        boolean eliminado = repoElementos.eliminar(clave);
        if (eliminado) catalogo.retirar(clave);
        return eliminado;
    }

    /** Devuelve el catálogo administrado por la fachada. */
    public CatalogoProcedimientos catalogo() {
        return catalogo;
    }

    /** Obtiene el procedimiento asociado o identificado. */
    public Procedimiento elemento(String clave) {
        return catalogo.buscar(CodigoClinico.parse(clave))
                .orElseThrow(() -> new ReglaNegocioException("No existe " + clave));
    }

    // ------------------------------------------------------------------ propietarios

    /** Valida el correo único, asigna identidad y persiste un propietario. */
    public synchronized Propietario registrarPropietario(String nombre, String correo, Nivel nivel) {
        Propietario p = new Propietario(propietarios.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1,nombre,correo,nivel);
        if (propietarios.values().stream().anyMatch(a -> a.correo().equals(p.correo()))) throw new ReglaNegocioException("El correo ya está registrado");
        repoPropietarios.guardar(p); propietarios.put(p.id(),p);
        publicar(TipoEvento.ALTA,"Propietario registrado: " + p.nombre());
        return p;
    }

    /** Cambia y persiste el nivel del propietario. */
    public synchronized void cambiarNivel(int idPropietario, Nivel nivel) {
        Propietario a = propietario(idPropietario);
        actualizarPropietario(idPropietario, a.nombre(), a.correo(), nivel);
    }

    /** Obtiene el propietario asociado o identificado. */
    public Propietario propietario(int id) {
        return Optional.ofNullable(propietarios.get(id))
                .orElseThrow(() -> new ReglaNegocioException("No existe el registro " + id));
    }

    /** Devuelve los propietarios ordenados por identificador. */
    public List<Propietario> propietarios() {
        return propietarios.values().stream().sorted(Comparator.comparingInt(Propietario::id)).toList();
    }

    // ------------------------------------------------------------------ atenciones

    /** Abre y persiste una atención con la fecha del reloj inyectado. */
    public synchronized Atencion abrir(int idPropietario) {
        Atencion t = new Atencion(registros.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1,propietario(idPropietario),reloj.get());
        repoRegistros.guardar(t); registros.put(t.id(),t);
        publicar(TipoEvento.CAMBIO_ESTADO,"Atención #" + t.id() + ": " + t.estado());
        return t;
    }

    /** Agrega y persiste un procedimiento con su cantidad de sesiones. */
    public synchronized Atencion.Linea agregarLinea(int idAtencion, String clave, int unidades) {
        Atencion t = registro(idAtencion);
        Atencion.Linea l = t.agregar(elemento(clave),unidades);
        try { repoRegistros.guardar(t); } catch (RuntimeException e) { t.quitar(l.elemento().clave()); throw e; }
        return l;
    }

    /** Quita y persiste una línea de una atención registrada. */
    public synchronized boolean quitarLinea(int idAtencion, String clave) {
        Atencion t = registro(idAtencion);
        CodigoClinico codigo = CodigoClinico.parse(clave);
        List<Atencion.Linea> anteriores = t.lineas();
        boolean ok = t.quitar(codigo);
        try { repoRegistros.guardar(t); }
        catch (RuntimeException e) {
            // Recupera también el orden original para conservar la vista de la atención.
            t.lineas().forEach(l -> t.quitar(l.elemento().clave()));
            anteriores.forEach(l -> t.agregar(l.elemento(), l.unidades()));
            throw e;
        }
        return ok;
    }

    /** Inicia la atención; exige al menos una línea y estado registrado. */
    public synchronized void iniciar(int idAtencion) {
        transitar(idAtencion, Atencion::iniciar);
    }

    /** Concluye la atención en curso. */
    public synchronized void cerrar(int idAtencion) {
        transitar(idAtencion, Atencion::cerrar);
    }

    /** Cancela una atención que no sea terminal. */
    public synchronized void cancelar(int idAtencion) {
        transitar(idAtencion, Atencion::cancelar);
    }

    /** Orden superior: la transición concreta llega como {@link Consumer} (referencia a método). */
    private void transitar(int id, Consumer<Atencion> transicion) {
        Atencion t = registro(id);
        Estado anterior = t.estado();
        transicion.accept(t);
        try { repoRegistros.guardar(t); } catch (RuntimeException e) { t.restaurarEstado(anterior); throw e; }
        publicar(TipoEvento.CAMBIO_ESTADO,"Atención #" + id + ": " + t.estado());
    }

    /** Busca una atención o informa que no existe. */
    public Atencion registro(int id) {
        return Optional.ofNullable(registros.get(id))
                .orElseThrow(() -> new ReglaNegocioException("No existe el registro " + id));
    }

    /** Devuelve las atenciones ordenadas por identificador. */
    public List<Atencion> registros() {
        return registros.values().stream().sorted(Comparator.comparingInt(Atencion::id)).toList();
    }

    /** Filtra las atenciones por estado. */
    public List<Atencion> registrosEn(Estado estado) {
        return registros().stream().filter(t -> t.estado() == estado).toList();
    }

    // ------------------------------------------------------------------ reportes (streams)

    private Stream<Atencion> cerrados() {
        return registros.values().stream().filter(t -> t.estado() == Estado.CERRADA);
    }

    /** Ingresos por subtipo (sólo registros cerrados). */
    public Map<String, Double> ingresosPorTipo() {
        return cerrados().flatMap(t -> t.lineas().stream()).collect(Collectors.groupingBy(l -> l.elemento().tipo(),TreeMap::new,Collectors.summingDouble(Atencion.Linea::importe)));
    }

    /** El procedimiento con más unidades en registros cerrados. */
    public Optional<Procedimiento> masDemandado() {
        return cerrados().flatMap(t -> t.lineas().stream()).collect(Collectors.groupingBy(Atencion.Linea::elemento,Collectors.summingInt(Atencion.Linea::unidades))).entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey);
    }

    /** Los {@code n} propietarios con mayor importe acumulado. */
    public List<Propietario> mejores(int n) {
        return cerrados().collect(Collectors.groupingBy(Atencion::propietario,Collectors.summingDouble(Atencion::total))).entrySet().stream().sorted(Map.Entry.<Propietario,Double>comparingByValue().reversed()).limit(n).map(Map.Entry::getKey).toList();
    }

    /** Resume los totales de las atenciones concluidas. */
    public DoubleSummaryStatistics estadisticas() {
        return cerrados().mapToDouble(Atencion::total).summaryStatistics();
    }

    /** Separa los procedimientos que alcanzan el umbral de precio. */
    public Map<Boolean, List<Procedimiento>> particionarPorPrecio(double umbral) {
        return catalogo.ordenados(Comparator.comparing(Procedimiento::clave)).stream().collect(Collectors.partitioningBy(e -> e.precioBase() >= umbral));
    }

    /** Agrupa y cuenta las atenciones por estado. */
    public Map<Estado, Long> conteoPorEstado() {
        return registros.values().stream()
                .collect(Collectors.groupingBy(Atencion::estado, () -> new TreeMap<>(), Collectors.counting()));
    }

    /** Busca entidades por identidad o criterio, según el contrato. */
    public List<Procedimiento> buscar(Regla<? super Procedimiento> regla) {
        return catalogo.filtrar(regla);
    }

    /** Proyección genérica (orden superior). */
    public <R> List<R> proyectar(Function<? super Procedimiento, ? extends R> funcion) {
        return catalogo.ordenados(Comparator.comparing(Procedimiento::clave)).stream().<R>map(funcion).toList();
    }

    /** Descripción con <em>pattern matching</em> exhaustivo sobre la jerarquía sellada (sin default). */
    public String describir(Procedimiento e) {
        return switch (e) {
            case Procedimiento.Consulta c when c.prioridad() == 3 -> "Consulta urgente: " + c.nombre();
            case Procedimiento.Consulta c -> "Consulta: " + c.nombre();
            case Procedimiento.Vacuna v -> "Vacuna: " + v.nombre() + " / " + v.biologico();
            case Procedimiento.Cirugia c -> "Cirugía: " + c.nombre() + " / " + c.horasQuirofano() + " horas";
        };
    }

    // ------------------------------------------------------------------ monitor

    /**
     * Un ciclo de revisión (síncrono y probado de forma determinista):
     * cancela registros ABIERTOS sin líneas con al menos un día de antigüedad y emite una
     * ALERTA (una sola vez) por cada registro EN_CURSO con más de {@link #PLAZO_DIAS} días.
     *
     * @return número de acciones realizadas
     */
    public synchronized int revisar() {
        int acciones = 0;
        for (Atencion t : registros()) {
            switch (t.estado()) {
                case ABIERTA -> {
                    if (t.lineas().isEmpty() && t.diasDesde(reloj.get()) >= 1) {
                        cancelar(t.id()); acciones++;
                        publicar(TipoEvento.CANCELACION_AUTOMATICA,"Atención sin procedimientos cancelada #" + t.id());
                    }
                }
                case EN_CURSO -> {
                    if (t.diasDesde(reloj.get()) > PLAZO_DIAS && alertados.add(t.id())) {
                        acciones++; publicar(TipoEvento.ALERTA,"Seguimiento pendiente de la atención #" + t.id());
                    }
                }
                case CERRADA, CANCELADA -> { }
            }
        }
        return acciones;
    }

    /** Inicia el seguimiento con la periodicidad positiva indicada en milisegundos. */
    public synchronized void iniciarMonitor(long periodoMs) {
        if (monitorActivo()) throw new IllegalStateException("El monitor ya está activo");
        monitor = new MonitorSeguimiento(periodoMs);
        monitor.iniciar("Monitor de seguimiento veterinario");
    }

    /** Interrumpe y espera la terminación del monitor. */
    public void detenerMonitor() throws InterruptedException {
        MonitorSeguimiento m;
        synchronized (this) {
            m = monitor;
        }
        if (m != null) {
            m.detener();
        }
    }

    /** Indica si existe un monitor ejecutándose. */
    public synchronized boolean monitorActivo() {
        return monitor != null && monitor.activa();
    }

    /**
     * Clase interna: ES UNA {@link TareaPeriodica} y TIENE ACCESO a la envolvente.
     */
    public final class MonitorSeguimiento extends TareaPeriodica {

        private MonitorSeguimiento(long periodoMs) {
            super(periodoMs);
        }

        @Override
        protected void ejecutarCiclo() {
            Clinica.this.revisar();
        }
    }

    // ------------------------------------------------------------------ respaldo

    /** Escribe un respaldo binario del catálogo, propietarios y atenciones. */
    public synchronized void exportarRespaldo(Path archivo) throws IOException {
        Respaldo.exportar(archivo, catalogo, propietarios(), registros());
    }

    /** Detiene el monitor (si está activo). Invocar al cerrar la aplicación. */
    @Override
    public void close() {
        try {
            detenerMonitor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (conexionPropia != null) conexionPropia.close();
    }
}
