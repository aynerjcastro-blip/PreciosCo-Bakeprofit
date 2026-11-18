# Correcciones y ensayo del procesador

## Cambios de código

1. `OfferScreening.screenSizeEvidence` conserva validaciones de identidad, precio,
   objetivo activo y categoría. Omite únicamente la aceptación por rango/unidad del
   objetivo para productos con GTIN comparable. Usa tamaños efectivos y overrides.
2. `SizeConflictCheck` consulta ofertas PENDING y UNCLASSIFIED de corridas cerradas
   o del staging legado. Un rechazo previo por unidad no oculta una contradicción.
   Las rechazadas conservan su motivo; las pendientes se marcan por conflicto.
3. La protección actúa antes de CREAR un maestro. No sanea maestros existentes,
   no reescribe precios y no reemplaza evidencias contradictorias por suposiciones.
4. Identidad secundaria distingue ausencia de candidatos por atributos, ausencia de
   candidatos compatibles y puntaje insuficiente. Conserva umbral, margen y límite.
   Los mapas incompatibles indican producto inexistente/inactivo, EAN no comparable,
   categoría, marca, presentación/unidad o nombre/variante. Nunca se reemplazan solos.
5. Los motivos anteriores permanecen en el staging histórico. El código nuevo no
   cambia esas razones hasta un reprocesamiento expresamente controlado.

## Fichas públicas comprobadas el 17 de noviembre de 2026

Se consultaron páginas públicas respetando robots.txt, sin APIs internas ni escrituras.

| Caso | Evidencia publicada | Decisión |
| --- | --- | --- |
| [Cetaphil AM](https://www.bellapiel.com.co/cetaphil-serum-advanced-am-50-ml-6999/p) | Título y SKU: 30 ml; EAN local 6999, itemId 7012; slug dice 50 ml. | El tamaño de 30 ml concuerda con la ficha actual. No cambiar a 50 por el slug. |
| [Cetaphil PM](https://www.bellapiel.com.co/cetaphil-serum-repair-pm-50-ml-8001/p) | Título y SKU: 30 ml; EAN local 8001, itemId 7013; slug dice 50 ml. | Mismo criterio. Esto no constituye verificación independiente del EAN canónico asociado. |
| [Aderma Cutis](https://www.cutis.com.co/aderma-barra-limpiadora/p) | Especificación de presentación: 100 ML, EAN 3282771111216. | La discrepancia está publicada; no es una conversión del procesador. |
| [Aderma Medipiel](https://www.medipiel.com.co/dermopan-barra-limpiadora-a-derma/p) | SKU: 100 gr, mismo EAN. | Bloquear creación automática. Confirmar etiqueta/fabricante antes de curar el maestro. |
| [CeraVe Cutis](https://www.cutis.com.co/cerave-limpia-crema-espuma-hidrata-326ml-3886/p) | SKU y URL dicen 326ml, especificación dice 300 ML; EAN 3337875743563. | Conflicto interno adicional: no elegir una cifra automáticamente. |
| [CeraVe Medipiel](https://www.medipiel.com.co/limpiadora-crema-espuma-hidratante-cerave/p) | SKU y contenido visible: 236 ml; mismo EAN. | Conservar rechazo por conflicto hasta verificación de etiqueta/fabricante. |

El `gtin` de JSON-LD de estas páginas puede ser un identificador interno rellenado
con ceros. No se utiliza para inventar equivalencias. El SKU real puede diferir del
código local recibido en `ean`; la clave de mapeo conserva la prioridad existente.

## Comprobaciones de datos

Ejecutar `scripts/auditar_identidad_y_presentaciones.sql`, que usa una transacción de
solo lectura. Revisar las cinco equivalencias incompatibles contra los atributos
crudos y el maestro. No borrar mapas ni bajar el umbral para hacerlos pasar.

Para cada contradicción documentar: EAN de etiqueta, presentación, unidad, URL,
fecha y evidencia del fabricante. Solo después definir una corrección curada.
Un override global requiere que el EAN represente una única presentación. Si el EAN
se reutiliza entre productos distintos, corregir o desactivar el listado afectado.
Cambiar un override no cambia un maestro que ya existe con otro tamaño.

## Pruebas y repetición del ensayo

Usar Java 25, PostgreSQL aislado con el esquema completo y las dos migraciones previas.
No hay una migración nueva para estas correcciones. No se usa ddl-auto=update/create.

```powershell
$env:STAGING_ENABLED = 'false'
$env:PRECIOSCO_STAGING_ENABLED = 'false'
$env:MAIL_ENABLED = 'false'
$env:APP_NOTIFICATIONS_MAIL_ENABLED = 'false'
# Definir SPRING_DATASOURCE_URL, USERNAME y PASSWORD de la base de pruebas y JWT_SECRET.
Set-Location backend
.\mvnw.cmd clean verify '-Dsurefire.excludedGroups='
```

Confirmar cero fallos, errores y pruebas omitidas en surefire-reports. La integración
reproduce 100 G frente a 100 ML tanto con la fila en gramos PENDING como ya rechazada:
no crea maestro, precio ni evento. Verifica también EAN esperado y unidad desconocida.

Conservar la base actual como evidencia. Restaurar OTRO clon desde el respaldo previo
al ensayo; así Aderma todavía no tiene un maestro que condicione el resultado. No
eliminar productos/precios para simular ese estado en una base compartida.

Ejecutar primero la auditoría con el procesador apagado. Hacer una corrida en ese clon,
repetir auditoría y comprobar que no se creó el maestro contradictorio ni sus precios.
Los demás rechazos de presentación requieren objetivos apropiados (G frente a ML),
no conversiones supuestas. Mantener la base de producción fuera del ensayo.

`run_id` nulo significa legado: las observaciones de horas distintas se procesan
individualmente. No asignarles un UUID común para elegir el precio mínimo entre ellas.
Para verificar deduplicación, generar una corrida nueva con los scrapers actualizados
en la base de ensayo y comprobar su cierre. La segunda ejecución del procesador, sin
ofertas nuevas, no debe añadir precios ni equivalencias.

La corrida general tiene retención y confirma cada oferta en su propia transacción;
Ctrl+C no revierte lo confirmado. Batch-size controla paginación, no un máximo de ingesta.
