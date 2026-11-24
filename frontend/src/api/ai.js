import { API_ORIGIN } from './http';
import { getToken } from '../utils/auth-helper';

/**
 * Envía un mensaje al chat de IA para una sección.
 * @param {{ section: string, message: string, history: Array }} payload
 * @returns {Promise<{ reply: string, queriesUsed: number, queriesLimit: number, plan: string }>}
 */
export async function sendAiMessage(payload) {
    const res = await fetch(`${API_ORIGIN}/api/ai/chat`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${getToken()}`,
        },
        body: JSON.stringify(payload),
    });

    if (!res.ok) {
        const body = await res.json().catch(() => null);
        const error = new Error(body?.message ?? body?.error ?? 'Error en el chat de IA');
        error.status = res.status;
        throw error;
    }
    return res.json();
}

/**
 * Consulta la cuota actual del usuario para consultas IA.
 * @returns {Promise<{ queriesUsed: number, queriesLimit: number, plan: string }>}
 */
export async function fetchAiQuota() {
    const res = await fetch(`${API_ORIGIN}/api/ai/quota`, {
        headers: { Authorization: `Bearer ${getToken()}` },
    });

    if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.message ?? 'No se pudo obtener la cuota');
    }
    return res.json();
}