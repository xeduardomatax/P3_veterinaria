# Control de Atenciones de una Clínica Veterinaria

**TecNM - Instituto Tecnológico de Orizaba | Maestría en Sistemas Computacionales**  
**Tecnologías de Programación · Miniproyecto Java 3**

Aplicación de escritorio para gestionar procedimientos veterinarios, propietarios y atenciones, desarrollada con **Java 21**, **Swing + FlatLaf**, **SQLite** y **Maven**. El proyecto sigue los requisitos RF01-RF08 del documento de 11 páginas y mantiene la organización de paquetes `mx.tecnm.orizaba.tp.veterinaria`.

## Capturas de pantalla

Estas imágenes pertenecen a la aplicación de escritorio incluida en este repositorio. GitHub las muestra directamente porque están guardadas en `docs/capturas/` y se usan rutas relativas.

### Gestión de atenciones

![Pantalla de atenciones: estados, procedimientos y totales](docs/capturas/atenciones.png)

### Catálogos del sistema

| Procedimientos | Propietarios |
|:--:|:--:|
| ![Pantalla del catálogo de procedimientos](docs/capturas/procedimientos.png) | ![Pantalla de propietarios y niveles de descuento](docs/capturas/propietarios.png) |

### Resultados y seguimiento

| Reportes | Bitácora de eventos |
|:--:|:--:|
| ![Pantalla de reportes de ingresos y estadísticas](docs/capturas/reportes.png) | ![Pantalla de eventos y seguimiento](docs/capturas/eventos.png) |

> **Nota:** las capturas ilustran la interfaz con los datos de ejemplo; se mantienen como archivos PNG para visualizarlas en GitHub sin instalar ni ejecutar el programa.


## 1. Requisitos para ejecutar

- JDK 21, con `java -version` y `javac -version` mostrando la versión 21.
- Apache Maven configurado en PATH (`mvn -version` debe usar Java 21).
- Conexión a Internet la primera vez para descargar `sqlite-jdbc 3.46.1.3`, FlatLaf 3.5.4 y los plugins Maven.
- Windows, Linux o macOS con escritorio gráfico. Se recomienda Visual Studio Code con Extension Pack for Java.

## 2. Compilación y apertura (Windows PowerShell)

Abre una terminal **dentro de la carpeta extraída** `P3_veterinaria` y ejecuta:

```powershell
mvn clean package
java -jar target/veterinaria-1.0.0.jar
```

El comando Maven compila con `-Xlint:all -Werror`, ejecuta las 14 pruebas de aceptación originales y las pruebas propias, y crea un JAR ejecutable con dependencias. `java -jar` abre la interfaz usando `datos.db` (en el directorio desde el que se ejecuta). **No borres `datos.db`**: contiene los datos solicitados. Para utilizar otra base: `java -jar target/veterinaria-1.0.0.jar otra-clinica.db`.

Comandos adicionales: `mvn -q compile`; `mvn -q exec:java` (14 pruebas originales); `mvn -q exec:java -Pdemo` (consola); `mvn -q compile exec:java -Pescritorio`; `mvn -q test` (pruebas originales y ampliadas). Más ayuda en [COMANDOS.md](COMANDOS.md).

## 3. Pantallas y requisitos

| Pantalla | Función | Requisito |
|---|---|---|
| Procedimientos | CRUD para Vacuna, Consulta y Cirugía; clave autogenerada VAC/CON/CIR-0001; respaldo y restauración del catálogo | RF01, RF06 |
| Propietarios | Alta, consulta, edición, eliminación y correo único; niveles General, Plan de salud y Rescatista | RF02 |
| Atenciones | Abrir, añadir/quitar líneas, iniciar, concluir, cancelar, eliminar y consultar montos | RF03, RF04 |
| Reportes | Ingresos por tipo, cerradas/no cerradas, estadísticas de tarifas y top N rentable | RF07 |
| Bitácora de eventos | Altas, cambios, cancelaciones automáticas y avisos en tiempo real | RF05, RF08 |

El modelo valida las reglas, no la interfaz. El monitor se ejecuta cada 30 segundos en la aplicación gráfica y se detiene de forma cooperativa al cerrar. La UI no accede a JDBC ni contiene SQL. Las operaciones potencialmente lentas corren fuera del hilo de eventos Swing (EDT) y actualizan los controles al finalizar.

## 4. Modelo de negocio

- Vacuna: tarifa = precio base por unidades; propiedad `biologico` no vacía.
- Consulta: tarifa = precio base por unidades por `(1 + 0.25 * (prioridad - 1))`; prioridad entre 1 y 3.
- Cirugía: dos primeras sesiones a tarifa completa y posteriores al 60 %; horas de quirófano positivas.
- Descuentos: General 0 %, Plan de salud 12 %, Rescatista 30 %.
- Estados: `ABIERTA` (Registrada) → `EN_CURSO` (En atención) → `CERRADA` (Concluida); desde Abierta o En curso se permite Cancelada. Las líneas se editan sólo en Abierta y para iniciar debe existir al menos una.
- Monitor: cancela una atención Abierta sin líneas al cumplir un día y emite una sola alerta por atención En curso con más de tres días durante la sesión activa.
- Códigos clínicos: tres letras mayúsculas, guion y exactamente cuatro dígitos (0001 a 9999); los subtipos requieren el prefijo correcto.

## 5. Arquitectura

- `modelo`: entidades, interfaces, enumeraciones, cálculo de tarifas y validaciones.
- `funcional`: reglas composables y fábrica genérica.
- `eventos`: publicación y escucha de eventos.
- `concurrencia`: hilo periódico cooperativo.
- `persistencia`: JDBC, repositorios, transacciones y serialización.
- `servicio`: fachada única `Clinica`, donde viven los casos de uso y reportes.
- `ui`: interfaz Swing que sólo utiliza la fachada y tipos del dominio.
- `app`: pruebas, demostración, datos de ejemplo y punto de entrada.

El catálogo se respalda en binario y su restauración **fusiona** procedimientos con el mismo tipo/clave sin borrar el resto de la base. La fusión utiliza una transacción SQLite. Hay un acoplamiento entre `modelo` y `funcional` en el contrato original `filtrar(Regla)`; se conserva esa firma, conforme a la prohibición del documento de modificar firmas públicas, y se declara la limitación A16 en lugar de ocultarla.

Swing + FlatLaf se eligió porque permite una aplicación instalable sin servidor, tablas, pestañas y cuadros de diálogo. Su limitación es que requiere escritorio gráfico; se resolvió la coordinación de hilos con `SwingWorker`, un ejecutor serializado y `SwingUtilities.invokeLater`.

## 6. Datos de ejemplo

`datos.db` contiene **6 procedimientos, 3 propietarios y 4 atenciones**, una Registrada, una En atención, una Concluida y una Cancelada. El archivo incluye las cuatro tablas del esquema solicitado. La fecha de las atenciones de ejemplo es 2026-10-27; la atención En curso genera aviso después del plazo al abrir la aplicación en una fecha posterior, sin cambiar su estado. Puedes generar una base nueva para practicar con `DatosEjemplo` o usar un archivo `.db` diferente para no afectar la evidencia.

## 7. Evidencias y entrega

- [Diagrama de clases editable](diagramas/modelo.puml) e [imagen](diagramas/modelo.png).
- [Diagrama de arquitectura](diagramas/arquitectura.puml) e [imagen](diagramas/arquitectura.png).
- Capturas visibles en la sección **Capturas de pantalla** de este README (archivos en [`docs/capturas/`](docs/capturas/)).
- [Bitácora de uso de IA](BITACORA_IA.md) (revisar su contenido y fechas antes de publicar).
- [Matriz de cumplimiento y validación](docs/CUMPLIMIENTO.md).
- [Comandos paso a paso](COMANDOS.md).
- [Guía para publicar el proyecto en GitHub](PUBLICAR_EN_GITHUB.md).
- El historial Git original se conserva, sin crear commits retrospectivos. Los cambios de esta entrega quedan **sin confirmar** para que el autor los revise antes de registrar su propio commit.

**Estado de pruebas:** La bitácora original documenta una ejecución Maven exitosa el 02/11/2026 con 14/14. En la revisión externa del 03/11/2026 se compiló todo el código Java con JDK 21, `-Xlint:all -Werror` usando una clase auxiliar temporal para el tema FlatLaf, y se aprobaron T01-T10 junto con una regresión de códigos. Aquí no fue posible repetir T11-T14 ni `mvn package` porque no están disponibles Maven ni el controlador SQLite JDBC. Esta limitación del entorno no equivale a una falla demostrada del proyecto; **repite `mvn clean package` en tu equipo antes de entregar**.

**Cambio en la modalidad:** No se incorpora material para grabar video. El documento original todavía menciona video y defensa oral en F3; la sustitución por una exposición con PDF debe ser aceptada por el docente.
