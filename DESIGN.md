---
version: alpha
name: PreciosCo
description: Comparador colombiano de precios por sección comercial.
colors:
  primary: "#FF9F1C"
  secondary: "#10B981"
  background: "#F8FAFC"
  surface: "#FFFFFF"
  text: "#0F172A"
  muted: "#475569"
typography:
  heading:
    fontFamily: "Hubot Sans, sans-serif"
  body:
    fontFamily: "Mona Sans, sans-serif"
rounded:
  sm: "8px"
  md: "16px"
  lg: "24px"
omitted:
  - section: spacing
    reason: Se conserva el espaciado de los CSS Modules existentes; no hay una escala global uniforme.
---

## Overview

Producto para comparar presentaciones y precios en Colombia. Mantener las tarjetas
con ofertas por tienda, las pestañas compactas y los filtros laterales de Search.
La firma visual es el ahorro entre tiendas, con naranja de marca y verde de ahorro.
No convertir el buscador en una portada promocional ni rediseñar pantallas ajenas.

## Colors

La fuente canónica de valores en ejecución es `frontend/src/styles/tokens.css`.
Los componentes consumen sus variables semánticas; este documento registra el tema
claro existente y no genera CSS. El tema oscuro continúa en ese mismo archivo.

## Typography

Hubot Sans para títulos y Mona Sans para contenido, con los fallbacks existentes.
Moneda COP y textos en español de Colombia. No cambiar la fuente por este ajuste.

## Layout

`Search.module.css` controla el buscador: resultados a la derecha, filtros a la
izquierda y panel plegable en móvil. Preservar scroll de documento y paginación.

## Elevation & Depth

Reutilizar las sombras y superficies existentes de tokens.css y los CSS Modules.

## Shapes

Controles de sector y búsqueda redondeados; radios de tarjetas del sistema actual.

## Components

Filtros por categoría: radios nativos. Tiendas: `StoreFilter`, con checkboxes nativos.
Precios: `PriceRangeFilter`. Paginación: `SearchPagination`. No duplicar estos controles.
El botón de limpiar búsqueda mantiene nombre accesible, foco y área de 44 px.

## Do's and Don'ts

Usar colores, foco y tipografía del sistema. Respetar loading/error/empty existentes.
No mostrar tiendas de otro sector ni resultados globales mientras se resuelve uno.
No ocultar precios de otras tiendas dentro de una comparación: ADR-02 los conserva.
