/*
 * Lo que NO viene de la base de datos: textos, orden y slug de la URL de cada sección.
 * Los números (productos, tiendas) y el id real vienen del backend, nunca de aquí.
 */

export const SECTIONS = [
    {
        slug: 'supermercados',
        rootName: 'Supermercado',
        title: 'Supermercados',
        description: 'Granos, aceites, lácteos, bebidas y aseo del hogar.',
        notice: null,
    },
    {
        slug: 'droguerias',
        rootName: 'Droguería',
        title: 'Droguerías',
        description: 'Productos de droguería de venta libre.',
        notice: 'La información es orientativa y no sustituye a un profesional de la salud.',
    },
    {
        slug: 'cuidado-facial',
        rootName: 'Cuidado facial',
        title: 'Cuidado facial',
        description: 'Productos para el cuidado de la piel del rostro.',
        notice: 'La información es orientativa y no sustituye a un profesional de la salud.',
    },
    {
        slug: 'repuestos',
        rootName: 'Repuestos',
        title: 'Repuestos',
        description: 'Repuestos para vehículos y motos, comparados por su referencia.',
        notice: 'Comparamos por referencia del fabricante. No verificamos la compatibilidad con un vehículo o moto.',
    },
];

/**
 * Clave para comparar nombres: sin tildes, en minúsculas y sin "s" final.
 * "Supermercado" y "Supermercados" dan la misma clave.
 * Es frágil a propósito: lo robusto es un campo `slug` en la tabla categories (mejora futura).
 */
export function sectionKey(text) {
    return text
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .trim()
        .replace(/s$/, '');
}