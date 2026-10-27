# Bitácora de uso de IA

## Registro Git del 27/10/2026

Herramienta: OpenAI Codex. Solicitud: organizar un commit significativo por grupo E02–E16. El usuario proporcionó su identidad de autor y autorizó configurar el repositorio local. Se organiza la incorporación del código existente por grupos según docs/PLAN_COMMITS.md, con fechas actuales y sin modificar las pruebas ni simular etapas de desarrollo anteriores. Esta incorporación temática no acredita el historial incremental previo solicitado por el docente. La validación final se realiza con Maven sobre el conjunto completo; no se afirma que las incorporaciones intermedias compilen por separado.

| Fecha | Herramienta | Propósito y prompt resumido | Incorporación y motivo | Límites |
|---|---|---|---|---|
| 16/10/2026 | OpenAI Codex | Realizar proyecto y documento, escritorio, conforme al PDF, sin extras. | Se leyeron las 11 páginas y se delimitó Java 21, SQLite 3.46.1.3, Swing/FlatLaf y cinco pantallas. | No se añadieron módulos de mascotas, inventario, agenda o autenticación. |
| 16/10/2026 | OpenAI Codex | Completar los 90 TODO conservando firmas y pruebas. | Implementación asistida de validaciones, tarifas, claves, estados, tipos internos, reglas, monitor, JDBC, serialización, fachada y reportes. Se incorpora por corresponder al PDF y superar las pruebas ejecutadas. | No se modificó PruebasModelo.java. La comprensión debe ser verificada por el estudiante. |
| 16/10/2026 | OpenAI Codex | UI de escritorio, RF06/RF07 y CRUD de E1. | SwingWorker, eventos en EDT, CRUD, respaldo/restauración del catálogo y reportes requeridos. | La restauración fusiona catálogo; no reemplaza la base. No se inventó historial Git. |
| 16/10/2026 | OpenAI Codex | Verificar y documentar. | Pruebas originales y adicionales, revisión de pantallas, datos de ejemplo, Javadoc, diagrama y documentación. | Video y defensa requieren participación del estudiante. No se contó con base_proyectos.zip. |
| 17/10/2026 | OpenAI Codex | Entregar los diagramas actualizados. | Revisión del código, actualización del modelo y arquitectura en DOT/PlantUML y generación de SVG/PNG. Se incorpora para reflejar la UI Swing y la implementación existente. | Los diagramas describen el código; no se modifica su arquitectura. Se señala el acoplamiento modelo/funcional que ya contiene el esqueleto. |

Se declara asistencia sustancial, incluida generación del modelo. La sección 11 del PDF exige comprender y poder reproducir o modificar los métodos en la defensa. Las pruebas aprobadas no acreditan por sí solas el dominio conceptual del estudiante.


## Revisión del 21/10/2026

Herramienta: OpenAI Codex. Solicitud: analizar PDF y seis comprimidos y adaptar el apartado 3. Se revisaron 105 archivos Java; se incorporaron captura efectivamente final en Acumulador, protección de reinicio durante detención de TareaPeriodica, Javadoc de FabricaElemento y prueba de regresión Maven. Se documenta la relación con cada fila de Booch. Se conserva por elección expresa del usuario la firma filtrar(Regla) y el ciclo modelo/funcional pendiente; no se presenta A16 como completo. No se añadieron reflexión ni metaprogramación, porque la tabla no las requiere. No se alteraron PruebasModelo ni datos.db.


## Memoria de evolución del 21/10/2026

Herramienta: OpenAI Codex. Solicitud: comparar el ZIP inicial y preparar un PDF sobre cambios y aprendizajes. Se compararon 29 fuentes iniciales con 35 actuales: 90 bloques completados, 27 archivos con cambios, dos sin cambios de contenido y seis añadidos. Se generó una memoria de 11 páginas y fuente editable. El orden por etapas se presenta como reconstrucción didáctica; los textos personales son propuestas para adaptar. Se conserva el pendiente de modularidad. No se modificó código Java ni datos.db. Se cita la verificación anterior de la sesión; no se declara una nueva ejecución de Maven durante esta elaboración.


## Depuración conforme al PDF — 27/10/2026

Herramienta: OpenAI Codex. Solicitud: analizar el PDF, eliminar lo innecesario y ajustar a sus requisitos. Se extrajeron sus 11 páginas. Se retiraron informes complementarios, sus generadores y el ejemplo comentado de JavaFX; los reportes de UI se limitaron a RF07. Se conservaron las firmas y pruebas originales, la base de ejemplo, los tipos exigidos, los diagramas y la bitácora. Se corrigió la recuperación en memoria ante fallo al quitar líneas y la actualización del nivel ante fallo de persistencia. Se mantiene explícita la limitación A16 para no presentar cumplimiento total sin cambiar las firmas originales.

La verificación detectó cero atenciones en datos.db; se agregaron cuatro con estados distintos, conservando los seis procedimientos y tres propietarios existentes. Se guardó una copia anterior en tmp/datos-antes-depuracion.db. Maven package aprobó 14/14 y las pruebas propias, incluidas regresiones de escrituras fallidas.
