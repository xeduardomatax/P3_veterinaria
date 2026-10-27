# Diagramas actualizados

Versión revisada contra el código Java del proyecto el **17 de octubre de 2026**.

| Diagrama | Imagen | Vectorial | Fuente Graphviz | Fuente PlantUML |
|---|---|---|---|---|
| Clases del dominio | [modelo.png](modelo.png) | [modelo.svg](modelo.svg) | [modelo.dot](modelo.dot) | [modelo.puml](modelo.puml) |
| Arquitectura de escritorio | [arquitectura.png](arquitectura.png) | [arquitectura.svg](arquitectura.svg) | [arquitectura.dot](arquitectura.dot) | [arquitectura.puml](arquitectura.puml) |

Los PNG/SVG se generan desde DOT. Las fuentes PlantUML son equivalentes en los conceptos representados, con diferente distribución: `modelo.puml` también incluye la fachada, persistencia y UI para ofrecer una vista de clases integrada. Se muestran miembros principales, sin enumerar cada getter, constructor o clase de biblioteca.

Cambios reflejados:

- Interfaz de escritorio `VentanaClinica`, Swing/FlatLaf, SwingWorker y eventos reenviados al EDT.
- Métodos de fachada para edición/eliminación, restauración del catálogo y reportes adicionales.
- Repositorios, conexión administrada, SQLite y respaldo binario.
- Herencia sellada, capacidades, composición, agregación y tipos internos.
- Una atención puede contener **0..*** líneas; cada línea pertenece a una atención.

La arquitectura muestra las dependencias existentes, incluido el acoplamiento entre `modelo` y `funcional`: el catálogo usa `Regla`, mientras `Reglas` y la fábrica usan `Procedimiento`. Es una relación presente en el esqueleto, aunque la especificación pide dependencias acíclicas. Este trabajo de diagramación no altera el código Java.

Para consultar detalles, abrir SVG en un navegador y ampliar. Para editar, usar DOT con Graphviz o PUML con PlantUML.

Con Graphviz instalado se pueden regenerar las imágenes desde la raíz del proyecto:

```powershell
dot -Tpng diagramas/modelo.dot -o diagramas/modelo.png
dot -Tpng diagramas/arquitectura.dot -o diagramas/arquitectura.png
dot -Tsvg diagramas/modelo.dot -o diagramas/modelo.svg
dot -Tsvg diagramas/arquitectura.dot -o diagramas/arquitectura.svg
```

En este entorno se generaron con Graphviz WebAssembly (`@viz-js/viz` 3.31.0) mediante `scripts/renderizar_diagramas.mjs`; los PNG se obtuvieron con PyMuPDF mediante `scripts/diagramas_png.py`. Estas herramientas de documentación no agregan dependencias a la aplicación Java.
