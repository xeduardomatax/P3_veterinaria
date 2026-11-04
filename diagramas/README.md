# Diagramas del proyecto

Los diagramas corresponden a la arquitectura utilizada para el Miniproyecto Java 3.

| Diagrama | Imagen lista para visualizar | Fuente editable |
|---|---|---|
| Modelo de clases | [modelo.png](modelo.png) | [modelo.puml](modelo.puml) |
| Arquitectura de paquetes y capas | [arquitectura.png](arquitectura.png) | [arquitectura.puml](arquitectura.puml) |

Las fuentes `.puml` pueden abrirse con la extensión PlantUML de Visual Studio Code. Se conservan solamente las fuentes editables y una imagen por diagrama para evitar duplicaciones. La relación `CatalogoProcedimientos` → `Regla` mantiene un acoplamiento entre paquetes documentado en `docs/CUMPLIMIENTO.md`, por la conservación de las firmas públicas del esqueleto original.
