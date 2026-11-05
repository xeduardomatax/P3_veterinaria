# BITÁCORA DE USO DE HERRAMIENTAS DE INTELIGENCIA ARTIFICIAL

**Proyecto:** Control de Atenciones de una Clínica Veterinaria  
**Materia:** Tecnologías de Programación — Miniproyecto Java 3  
**Periodo documentado:** del 7 de octubre al 4 de noviembre de 2026  
**Herramientas registradas:** ChatGPT y OpenAI Codex  

Esta bitácora es una **reconstrucción retrospectiva** de las consultas y revisiones que quedaron documentadas durante el desarrollo. Los textos de las solicitudes son resúmenes, no transcripciones literales. Se registra tanto la orientación recibida como la ayuda utilizada para revisar o generar fragmentos de código. El objetivo es dejar claro para qué se utilizaron las herramientas y cuáles decisiones se conservaron en el proyecto.

## Semana 1 · Del 7 al 13 de octubre

### 7 de octubre — Preparación del entorno y organización inicial

- **Herramienta:** ChatGPT.
- **Propósito:** Aclarar cómo empezar el miniproyecto en Visual Studio Code y cómo organizar el trabajo con Java, Maven y GitHub.
- **Solicitud resumida:** «Quiero desarrollar el proyecto de manera sencilla, en Java y desde Visual Studio Code. ¿Cómo compruebo las herramientas y cómo puedo apoyarme en Codex sin perder el control del código?»
- **Orientación recibida:** Diferencias entre JDK y Maven; comprobación del entorno; organización del proyecto por paquetes; relación entre editor, repositorio Git y asistente de programación.
- **Qué se aceptó y por qué:** Se tomó como base un proyecto Maven y una aplicación de escritorio, porque permiten centrarse en los requisitos del modelo sin desplegar un servidor web.
- **Qué se descartó y por qué:** No se plantearon módulos de inventario, agendas o autenticación: no forman parte del problema especificado.

## Semana 2 · Del 14 al 20 de octubre

### 15–16 de octubre — Modelo de objetos y reglas de negocio

- **Herramientas:** ChatGPT y OpenAI Codex.
- **Propósito:** Entender el enunciado, distribuir las responsabilidades de las clases y resolver las partes más complejas del modelo.
- **Solicitud resumida:** «Analiza el PDF y ayúdame a completar las clases sin cambiar sus firmas: procedimientos, propietarios, atenciones, claves y validaciones».
- **Orientación y asistencia:** Uso de `Procedimiento` como clase abstracta sellada, subtipos `Vacuna`, `Consulta` y `Cirugia`, tarifas polimórficas, identificadores `CodigoClinico`, descuentos por nivel y transiciones válidas de `Atencion`. Se utilizó asistencia para explicar, revisar e implementar métodos de los grupos E02–E08.
- **Qué se aceptó y por qué:** Se mantuvieron las fórmulas de tarifas, la máquina de estados y las validaciones indicadas por el docente, ya que son parte de las pruebas de aceptación.
- **Qué se descartó y por qué:** No se modificaron las firmas públicas ni `PruebasModelo.java`, para conservar el contrato original del ejercicio.

### 16–17 de octubre — Componentes avanzados y diagramas

- **Herramienta:** OpenAI Codex.
- **Propósito:** Revisar conceptos de Java que no eran evidentes al leer únicamente las pantallas y documentar sus relaciones.
- **Solicitud resumida:** «Explícame y revisa los tipos internos, lambdas, hilos, repositorios y diagramas de clases de lo que realmente está implementado».
- **Orientación y asistencia:** Clases internas, interfaces funcionales, referencias a métodos, `streams`, monitor periódico, JDBC, serialización y relaciones entre paquetes. Se prepararon diagramas editables acordes con el proyecto.
- **Qué se aceptó y por qué:** Mantener separados los paquetes del modelo, servicio, persistencia e interfaz ayuda a identificar qué clase cambiar ante una nueva regla.
- **Límite reconocido:** Los diagramas sirven para describir el código; por sí solos no comprueban que toda dependencia de paquetes sea correcta.

## Semana 3 · Del 21 al 27 de octubre

### 21 de octubre — Revisión de concurrencia y programación funcional

- **Herramienta:** OpenAI Codex.
- **Propósito:** Revisar errores y decisiones de diseño en funciones de mayor dificultad.
- **Solicitud resumida:** «Comprueba que las reglas funcionales, los tipos internos y el hilo de seguimiento estén bien utilizados y que se puedan explicar en la evaluación».
- **Orientación y asistencia:** Revisión de `Regla<T>`, clase local `Acumulador`, captura de variables efectivamente finales y detención cooperativa de `TareaPeriodica`.
- **Qué se aceptó y por qué:** Se incorporaron ajustes de seguridad del hilo y documentación de métodos para reducir comportamientos inesperados y facilitar la explicación.
- **Qué quedó pendiente:** Se identificó una limitación de dependencia entre `modelo` y `funcional` respecto al criterio A16; no se presenta como resuelta.

### 27 de octubre — Interfaz, persistencia y pruebas

- **Herramienta:** OpenAI Codex.
- **Propósito:** Relacionar la interfaz Swing con la fachada `Clinica` y revisar persistencia, monitor y casos de demostración.
- **Solicitud resumida:** «Verifica el flujo de atenciones desde la interfaz, los datos de ejemplo y los resultados de las pruebas sin alterar el modelo exigido».
- **Orientación y asistencia:** Revisión del CRUD de procedimientos y propietarios, atenciones con líneas y estados, reportes, bitácora de eventos, respaldo binario y operaciones SQLite.
- **Qué se aceptó y por qué:** Las acciones de la interfaz pasan por el servicio; los mensajes del monitor se muestran sin bloquear el hilo gráfico. Se conservaron datos de ejemplo y pruebas del docente.
- **Verificación documentada en esa etapa:** En la revisión del 27 de octubre se registró la ejecución de 14/14 pruebas con Maven. Esa ejecución no sustituye repetirlas sobre la entrega definitiva.

## Semana 4 · Del 28 de octubre al 3 de noviembre

### 2 de noviembre — Depuración del proyecto conforme a la rúbrica

- **Herramienta:** OpenAI Codex.
- **Propósito:** Comparar el resultado con las 11 páginas del PDF y retirar elementos innecesarios.
- **Solicitud resumida:** «Revisa lo que aún no corresponde a la rúbrica, conserva las pruebas originales y simplifica la entrega».
- **Orientación y asistencia:** Revisión de transacciones SQLite y del monitor, ajuste de la base de ejemplo a cuatro estados distintos, documentación de ejecución desde PowerShell y limpieza de archivos auxiliares.
- **Qué se aceptó y por qué:** Conservar la base SQLite, los diagramas, el código necesario y el historial auténtico de Git, sin inventar avances ni cambios anteriores.
- **Qué no se declaró como resuelto:** El criterio A16 sigue documentado como limitación; no se cambió el contrato público para ocultarlo.
- **Verificación documentada en esa etapa:** Se registró una compilación Maven y 14/14 pruebas aprobadas en el entorno donde se realizó esa revisión.

### 3 de noviembre — Ajustes finales y preparación para exponer

- **Herramienta:** ChatGPT.
- **Propósito:** Dar una última revisión a la entrega y preparar material independiente de exposición, ya que no se realizará el video.
- **Solicitud resumida:** «Analiza a detalle el ZIP y el PDF, elimina lo que no se necesita, conserva lo solicitado y dame un documento que pueda usar para explicar el proyecto».
- **Orientación y asistencia:** Validación adicional del formato de `CodigoClinico`, revisión de las pruebas, depuración de archivos temporales y preparación del PDF de exposición.
- **Qué se aceptó y por qué:** Se mantuvo la arquitectura y las funciones requeridas; se dio prioridad a cambios puntuales en lugar de añadir funciones ajenas a la rúbrica.
- **Qué se descartó y por qué:** No se creó un video ni se simuló que ya se había realizado la defensa oral.
- **Límite de la comprobación final:** Con JDK 21 se verificó la compilación y se ejecutaron T01–T10 más una prueba adicional. T11–T14 requieren repetir la ejecución con la dependencia SQLite JDBC disponible. Por ello, no se afirma que las 14 pruebas hayan sido ejecutadas nuevamente el 3 de noviembre.

## Actualización posterior a la cuarta semana · 4 de noviembre de 2026

### 4 de noviembre — Revisión y actualización de diagramas UML

- **Herramienta:** ChatGPT.
- **Propósito:** Comprobar que los diagramas del proyecto correspondieran a las clases, relaciones y paquetes de la versión actual del código Java, y corregir las diferencias antes de entregar.
- **Solicitud resumida:** «Revisa si el diagrama de clases está actualizado conforme a mi proyecto; si no lo está, actualízalo y entrégame los diagramas en formatos PNG y PUML».
- **Orientación y asistencia:** Se compararon los diagramas anteriores con los archivos Java. Se identificó la ausencia de `InicioEscritorio` y una dependencia representada incorrectamente entre `Aplicacion` y `Clinica`. Con apoyo de ChatGPT se actualizaron el diagrama de clases del modelo, el de servicio y persistencia, y el de arquitectura y dependencias. También se generaron sus versiones visuales `.png` y los archivos editables `.puml` de PlantUML.
- **Qué se aceptó y por qué:** Se conservaron los tres diagramas actualizados y sus fuentes editables dentro de la carpeta `diagramas/`, y se enlazaron desde el `README.md`, porque permiten explicar mejor la estructura real del sistema y cumplen el requisito de entregar un diagrama de clases actualizado.
- **Qué se corrigió:** Se incorporó `InicioEscritorio` y se ajustaron las relaciones de inicio, servicio, modelo y persistencia para reflejar el código existente.
- **Qué no se modificó y por qué:** No se alteró la lógica de las clases Java ni la base de datos durante esta actualización, ya que el objetivo fue corregir la documentación técnica.
- **Límite reconocido:** Los diagramas reflejan la dependencia circular detectada entre `modelo` y `funcional`; esta observación sobre el criterio A16 sigue pendiente de corregirse en el código.

## Reflexión final

Las herramientas de IA resultaron especialmente útiles al estudiar **polimorfismo y tarifas, transiciones de estado, concurrencia, JDBC, serialización, reportes con streams y documentación de diagramas UML**. Además de la orientación conceptual, hubo asistencia para revisar y generar código, por lo que estos apartados requieren estudio y práctica antes de la defensa.

La explicación del diseño, la comprensión de los métodos y la capacidad de modificar el programa durante la evaluación corresponden al estudiante. Este documento declara el apoyo utilizado y las limitaciones que se conocen, sin atribuir trabajo a herramientas no verificadas.
