import { useSections } from "./useSection";

/**
 * Busca una sección por el slug de la URL ("supermercados", "drogueria"...).
 * Lo usan la página de sección y el buscador, para traducir ?sector= al id real.
 *
 * `section` es undefined si el slug no existe (o si todavía está cargando):
 * por eso hay que mirar `status` antes de decidir "no encontrada".
 */
export function useSectionBySlug(slug) {
    const { status, sections, stats, reload } = useSections();
    const section = slug ? sections.find((s) => s.slug === slug) : undefined;
    return { status, section, stats, reload };
}