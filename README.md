# Control de Atenciones de una Clínica Veterinaria

Aplicación de escritorio del Miniproyecto Java 3, basada en los requisitos RF01–RF08 y la lista de cotejo del PDF. Java 21, Maven, SQLite JDBC 3.46.1.3 y paquete raíz `mx.tecnm.orizaba.tp.veterinaria`.

## Compilar y ejecutar

Instala JDK 21 y Maven. La primera compilación necesita descargar las dependencias.

```powershell
mvn -q package
java -jar target/veterinaria-1.0.0.jar
```

Para abrir desde Maven:

```powershell
mvn -q compile exec:java -Pescritorio
```

Se utiliza `datos.db` en el directorio de ejecución. La base de ejemplo contiene al menos 6 procedimientos, 3 propietarios y 4 atenciones en estados distintos. Para trabajar en otra base:

```powershell
java -jar target/veterinaria-1.0.0.jar mi-clinica.db
```

Comandos del documento:

```powershell
mvn -q compile
mvn -q exec:java
mvn -q exec:java -Pdemo
```

`exec:java` ejecuta las 14 pruebas originales. `mvn -q test` ejecuta además integración y concurrencia; las pruebas usan bases temporales. Las firmas públicas y las pruebas originales se conservan.

## Funciones requeridas

| Requisito | Operación |
|---|---|
| RF01 | Procedimientos: registrar, consultar, modificar y eliminar vacunas, consultas y cirugías con clave automática. |
| RF02 | Propietarios: nombre, correo único y nivel General, Plan de salud o Rescatista. Edición y eliminación para E1. |
| RF03 | Atenciones: abrir, agregar/quitar líneas, iniciar, concluir o cancelar. Eliminación para E1. |
| RF04 | Subtotal por tarifa polimórfica y total con descuento de 0 %, 12 % o 30 %. |
| RF05 | Monitor: cancelar registradas sin líneas desde un día y alertar una vez las que están en atención por más de tres días. |
| RF06 | SQLite al reiniciar; respaldo binario y restauración del catálogo. |
| RF07 | Ingresos por tipo, concluidas/no concluidas, estadísticas de tarifas de una sesión y N procedimientos más rentables. |
| RF08 | Cinco pantallas y bitácora de eventos en tiempo real mediante Clinica. |

Las líneas sólo se editan en estado Registrada. Para iniciar se requiere al menos una línea. Concluida y Cancelada son terminales. No se eliminan procedimientos ni propietarios referenciados por atenciones.

Las tarifas siguen la sección 6: vacunas por dosis; consultas con 25 % adicional por nivel de prioridad; cirugías con las sesiones posteriores a la segunda al 60 %. `tarifaConImpuesto` aplica 16 %; los totales de atención siguen el descuento y las pruebas proporcionadas.

El respaldo conserva el formato original: catálogo, propietarios y atenciones. **Restaurar catálogo** agrega claves ausentes y actualiza las coincidentes del mismo tipo. Conserva los procedimientos adicionales y las atenciones actuales; no restaura toda la base.

## Interfaz y diseño

Swing + FlatLaf ofrece tablas y formularios sin servidor. Requiere un entorno gráfico y administrar explícitamente el hilo de eventos. Las consultas, escrituras y archivos se ejecutan mediante SwingWorker en un ejecutor de un hilo; las actualizaciones visuales se realizan en el EDT y los eventos usan SwingUtilities.invokeLater. El cierre detiene el monitor con interrupt/join fuera del EDT y cierra SQLite.

Se abre una única Clinica. La UI consume sus casos de uso y los tipos del modelo; no ejecuta SQL ni crea JDBC. El monitor de la ventana revisa cada 30 segundos; la fachada permite configurar el período e inyectar Supplier<LocalDate> para pruebas.

Se mantienen la jerarquía sellada, los tipos internos, las interfaces funcionales, los bloques de inicialización, los repositorios con transacciones y batch, Serializable, transient y Externalizable exigidos por el PDF.

**Limitación A16:** el contrato original `CatalogoProcedimientos.filtrar(Regla)` introduce una dependencia modelo → funcional, mientras Reglas y FabricaElemento dependen del modelo. Se conserva para respetar las firmas públicas del apartado 7.1; por ello no se declara cumplido el requisito de dependencias acíclicas. Resolverlo requiere autorizar un cambio de contrato o de ubicación de tipos.

## Entregables

- Diagrama actualizado: [modelo](diagramas/modelo.png), [fuente PlantUML](diagramas/modelo.puml) y [arquitectura](diagramas/arquitectura.png).
- [Bitácora de uso de IA](BITACORA_IA.md).
- [Guion de video y defensa](docs/GUION_VIDEO.md).
- Base de ejemplo: `datos.db`.

El historial Git incremental, el video de hasta cinco minutos y la defensa oral requieren el trabajo real del estudiante. No se fabrica historial retrospectivo.

## Capturas

![Procedimientos](docs/capturas/procedimientos.png)
![Propietarios](docs/capturas/propietarios.png)
![Atenciones](docs/capturas/atenciones.png)
![Reportes](docs/capturas/reportes.png)
![Eventos](docs/capturas/eventos.png)
