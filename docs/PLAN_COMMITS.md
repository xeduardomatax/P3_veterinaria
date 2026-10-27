# Registro inicial por grupos E02–E16

El repositorio no tenía commits al revisar el 27/10/2026. El código ya estaba implementado. Los commits que se creen ahora deben describirse como incorporación o revisión del código existente, con su fecha real. No constituyen evidencia de que el desarrollo original se realizó en quince etapas.

No se deben alterar fechas, inventar etiquetas de semanas pasadas ni afirmar que cada versión intermedia fue compilada si no se comprobó. La evaluación de este registro inicial frente al historial incremental solicitado corresponde al docente.

## Organización propuesta

| Grupo | Contenido que se incorpora o revisa | Mensaje de commit |
|---|---|---|
| E01 | pom.xml, .gitignore, compilar.sh, probar.sh, lib/LEEME.txt | build(E01): incorporar configuración Java 21 y SQLite |
| E02 | modelo/Facturable.java | feat(E02): incorporar contrato de facturación existente |
| E03 | modelo/CodigoClinico.java | feat(E03): incorporar identidad clínica y secuencias |
| E04 | modelo/Procedimiento.java, modelo/ReglaNegocioException.java | feat(E04): incorporar base sellada y validaciones |
| E05 | modelo/Programable.java, modelo/Supervisado.java | feat(E05): incorporar capacidades de los subtipos |
| E06 | modelo/Nivel.java, modelo/Propietario.java | feat(E06): incorporar propietarios y descuentos |
| E07 | modelo/Estado.java, modelo/Atencion.java | feat(E07): incorporar estados y composición de atenciones |
| E08 | app/PruebasModelo.java, app/DemoModelo.java | test(E08): incorporar aceptación original y demostración de totales |
| E09 | modelo/CatalogoProcedimientos.java | feat(E09): incorporar catálogo e iterador privado |
| E10 | diagramas/ y scripts de diagramas | docs(E10): incorporar diagramas de tipos internos y arquitectura |
| E11 | funcional/ | feat(E11): incorporar reglas y fábricas funcionales |
| E12 | concurrencia/, eventos/, prueba de reinicio del hilo | feat(E12): incorporar tarea periódica y eventos |
| E13 | persistencia/ excepto Respaldo.java | feat(E13): incorporar repositorios SQLite y transacciones |
| E14 | persistencia/Respaldo.java | feat(E14): incorporar respaldo por serialización |
| E15 | servicio/Clinica.java, app/DatosEjemplo.java, datos.db | feat(E15): incorporar fachada y base de ejemplo |
| E16 | ui/, app/Aplicacion.java, pruebas de integración y escritorio | feat(E16): incorporar interfaz de reportes y verificación de integración |
| Entrega | README.md, TODO.md, BITACORA_IA.md, docs/ | docs: registrar instrucciones y evidencia de entrega |

Las rutas Java son relativas a `src/main/java/mx/tecnm/orizaba/tp/veterinaria/`; las pruebas de escritorio y reinicio están bajo `src/test/java/`.

Hay archivos que implementan varios grupos: Procedimiento contiene E04/E05; Atencion, E07/E08; CatalogoProcedimientos, E09/E10; Propietario, E06/E14; Clinica, E15/E16 y monitor. El código de cada archivo debe incorporarse completo, sin deshacer métodos para simular su implementación posterior. Los mensajes anteriores describen el contenido principal, no la autoría cronológica de cada método. Las incorporaciones intermedias pueden depender de archivos aún no incorporados.

## Verificación final

```powershell
mvn -q package
git status --short
git log --reverse --format="%h %ad %s" --date=short
```

El estado final debe contener los archivos de entrega y excluir target/, tmp/, herramientas locales y archivos temporales. Las pruebas originales no se editan para crear diferencias artificiales. Los cambios futuros se registran cuando realmente se realizan, con su propósito y validación.
