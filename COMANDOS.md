# Guía de ejecución en Visual Studio Code / PowerShell

1. Extrae el ZIP y abre **la carpeta `P3_veterinaria`**, no el ZIP ni la subcarpeta `src`, en Visual Studio Code.
2. Abre **Terminal → Nuevo terminal**. Comprueba que el terminal esté en la carpeta que contiene `pom.xml` y `datos.db`.
3. Verifica los programas:

```powershell
java -version
javac -version
mvn -version
```

Todos deben indicar JDK 21 para compilar el proyecto. Conecta tu equipo a Internet en la primera compilación para descargar las dependencias.

## Compilar y abrir

```powershell
mvn clean package
java -jar target/veterinaria-1.0.0.jar
```

**Importante:** no ejecutes `java -jar` desde la carpeta `target`; ejecútalo desde la carpeta principal para usar el `datos.db` de ejemplo.

## Otras opciones

| Objetivo | Comando |
|---|---|
| Compilar sin empaquetar | `mvn -q compile` |
| Ejecutar 14 pruebas originales | `mvn -q compile exec:java` |
| Ejecutar pruebas originales, adicionales y empaquetar | `mvn clean package` |
| Iniciar la ventana con Maven | `mvn -q compile exec:java -Pescritorio` |
| Ejecutar la demostración de consola | `mvn -q compile exec:java -Pdemo` |
| Generar Javadoc | `mvn javadoc:javadoc` |
| Utilizar otra base | `java -jar target/veterinaria-1.0.0.jar practica.db` |
| Crear una nueva base de ejemplo, sin reemplazar otra | `java -cp target/veterinaria-1.0.0.jar mx.tecnm.orizaba.tp.veterinaria.app.DatosEjemplo ejemplo-nuevo.db` |

## Demostración sugerida

Abre Procedimientos, luego Propietarios. En Atenciones, selecciona la atención Concluida y examina el descuento, después revisa Reportes y Bitácora de eventos. Para demostrar altas o transiciones sin modificar la base de entrega, abre una base de prácticas. El monitor se detiene cuando cierras la ventana.

## Si aparece un error

- `mvn` no se reconoce: instala Apache Maven y reinicia la terminal.
- Se usa Java 26 u otra versión: configura `JAVA_HOME` para **JDK 21** y verifica `mvn -version`.
- No encuentra SQLite JDBC o FlatLaf: asegúrate de contar con conexión a Internet y ejecuta `mvn clean package` desde el proyecto raíz.
- `no main manifest attribute` o el JAR no abre: termina correctamente `mvn clean package` y usa el nombre `target/veterinaria-1.0.0.jar`.
- La base no contiene información: revisa que estás ejecutando desde la carpeta que incluye `datos.db`.

**Alternativa sin Maven:** `compilar.sh` y `probar.sh` requieren Bash y los JAR indicados en `lib/LEEME.txt`. Para Windows con PowerShell se recomienda Maven, que resuelve automáticamente todas las dependencias.
