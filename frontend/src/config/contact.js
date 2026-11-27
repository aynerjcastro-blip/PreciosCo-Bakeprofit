/**
 * Correo de contacto del equipo (cuenta de Gmail a crear: preciosco.contacto@gmail.com).
 * Se usa en /contacto, los términos y la política de privacidad. Se puede cambiar sin tocar el
 * código con VITE_CONTACT_EMAIL en frontend/.env.
 */
export const CONTACT_EMAIL = import.meta.env.VITE_CONTACT_EMAIL?.trim() || 'preciosco.contacto@gmail.com';

/** Fecha de vigencia de los términos y la política de privacidad. */
export const LEGAL_UPDATED = '27 de noviembre de 2026';
