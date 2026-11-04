# Matriz de revisión y límites de la entrega

**Fuente:** `P3_veterinaria(2).pdf`, páginas 1 a 11. **Revisión:** 03/11/2026. No se modifican las firmas de las pruebas de aceptación originales.

## Requisitos funcionales

| ID | Evidencia en el código |
|---|---|
| RF01 | `Procedimiento`, `CodigoClinico`, `RepositorioProcedimientoSQLite`, catálogo y UI |
| RF02 | `Propietario`, `Nivel`, repositorio y formulario de propietarios |
| RF03 | `Atencion`, `Estado` y UI de atenciones/líneas |
| RF04 | `Procedimiento.tarifa`, `calcularTarifa` de los subtipos, `Linea.importe` y `Atencion.total` |
| RF05 | `TareaPeriodica`, `Clinica.MonitorSeguimiento`, `Clinica.revisar` y oyentes de eventos |
| RF06 | `ConexionSQLite`, tres repositorios, `Respaldo`, datos.db |
| RF07 | `Clinica.ingresosPorTipo`, `particionarAtenciones`, `estadisticasTarifas`, `masRentables` |
| RF08 | `ui.InicioEscritorio`, `ui.VentanaClinica`, eventos en EDT |

## E01–E16 y rúbrica

Los grupos E01-E16 se encuentran implementados en `src/main/java`. Los modelos incluyen herencia sellada, interfaces de capacidades, composición y agregación, clases internas, interfaz funcional y fábrica, colecciones con streams, JDBC transaccional y serialización. Los 14 métodos de `PruebasModelo` están intactos. Adicionalmente se incluyen pruebas de integración, reinicio del hilo, restauración atómica y formato de código clínico.

**Pendiente conocido A16 (modularidad):** `modelo.CatalogoProcedimientos` declara un método público que recibe `funcional.Regla`. A la vez, `funcional.Reglas` y `FabricaElemento` importan `modelo`. Esto constituye una dependencia entre paquetes en ambos sentidos. Se conserva la firma por la instrucción expresa de la sección 7.1 de no modificar firmas públicas. No se presenta A16 como 100 % logrado.

**Seguimiento:** las alertas únicas se recuerdan por la sesión de `Clinica` y no en la base de datos. Esto no equivale a una marca persistente entre ejecuciones. El esquema SQLite de cuatro tablas se respeta sin columnas adicionales.

**Restauración:** el respaldo es binario (incluye catálogo, propietarios y atenciones); la acción de la UI restaura/fusiona solamente el catálogo, preservando los otros datos actuales. La restauración se hace en un lote transaccional.

## Datos de ejemplo comprobados en la revisión

| Tabla | Filas |
|---|---:|
| procedimiento | 6 |
| propietario | 3 |
| atencion | 4 (ABIERTA, EN_CURSO, CERRADA, CANCELADA) |
| atencion_linea | 4 |

SQLite (biblioteca estándar de Python) informó `PRAGMA integrity_check = ok` y cero violaciones en `PRAGMA foreign_key_check`. Esta inspección valida el archivo `datos.db`, no sustituye las pruebas Java.

## Pruebas disponibles

- **Historial documentado previamente en la bitácora, 02/11/2026:** `mvn -B clean package`, resultado reportado BUILD SUCCESS con 14/14 y pruebas de integración aprobadas. No se repitió aquí una ejecución de Maven.
- **Revisión de código fuente, 03/11/2026:** compilación `javac --release 21 -Xlint:all -Werror` de todos los archivos Java exitosa. Se usó exclusivamente en el entorno de evaluación un stub temporal de `FlatLightLaf`, **no incluido en el ZIP**.
- **Pruebas originales ejecutadas localmente:** T01-T10 aprobadas. T11-T14 no se pudieron ejecutar aquí por ausencia de `sqlite-jdbc` en el entorno local. La prueba adicional `PruebaValidaciones` sí fue aprobada.
- **Pendiente antes de la entrega:** ejecutar `mvn clean package` con Maven y dependencias accesibles y, si es posible, hacer una inspección visual de la interfaz y probar la operación de respaldo.

## Entregables / modalidad

Se conserva código, README, Javadoc, plantillas PUML, imágenes de diagramas, bitácora IA, SQLite de ejemplo y el **historial Git auténtico**. El proyecto incluye cambios sin confirmar: se deben revisar y registrar de forma transparente antes de publicar en el repositorio. Se retiró la carpeta de herramientas temporales, generadores auxiliares, documentos de preparación del video y materiales duplicados.

**La exposición se apoya en un PDF externo**, en lugar de un video grabado. El documento original (página 11) aún asigna 3 puntos a video y defensa; sólo el docente puede confirmar si sustituirlo por PDF modifica la evaluación.
