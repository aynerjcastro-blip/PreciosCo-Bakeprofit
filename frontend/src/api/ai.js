import { API_ORIGIN } from './http';
import { getToken } from '../utils/auth-helper';

/**
 * Envía un mensaje al chat de IA para una sección.
 * @param {{ section: string, message: string, history: Array }} payload
 * @returns {Promise<{ reply: string }>}
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
        const error = new Error(body?.error ?? 'Error en el chat de IA');
        error.status = res.status;
        throw error;
    }
    return res.json();
}