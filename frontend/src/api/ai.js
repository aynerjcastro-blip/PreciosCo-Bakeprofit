import { API_ORIGIN } from './http';
import { getToken } from '../utils/auth-helper';

/**
 * Envía un mensaje al chat de IA con streaming SSE.
 * Llama al callback `onChunk` por cada fragmento de texto que llega.
 *
 * @param {{ section: string, message: string, history: Array }} payload
 * @param {(text: string) => void} onChunk — callback por cada fragmento
 * @returns {Promise<{ queriesUsed: number, queriesLimit: number, plan: string }>}
 *          Resuelve con la info de cuota cuando el stream termina.
 */
export async function streamAiMessage(payload, onChunk) {
    const res = await fetch(`${API_ORIGIN}/api/ai/chat/stream`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${getToken()}`,
            // application/json: los errores 400/429 llegan como JSON antes del stream
            Accept: 'text/event-stream, application/json',
        },
        body: JSON.stringify(payload),
    });

    if (!res.ok) {
        const body = await res.json().catch(() => null);
        const error = new Error(body?.message ?? body?.error ?? 'Error en el chat de IA');
        error.status = res.status;
        throw error;
    }

    // Leer el stream SSE como texto
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let quotaInfo = null;
    let buffer = '';
    // Fuera del bucle: "event:" y "data:" pueden llegar en lecturas distintas
    let currentEvent = null;

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // Procesar líneas completas del buffer
        const lines = buffer.split('\n');
        // Guardar la última línea incompleta para la siguiente iteración
        buffer = lines.pop() || '';

        for (const rawLine of lines) {
            const line = rawLine.replace(/\r$/, '');

            if (line === '') {
                currentEvent = null; // línea vacía = fin del evento SSE
            } else if (line.startsWith('event:')) {
                currentEvent = line.slice('event:'.length).trim();
            } else if (line.startsWith('data:')) {
                const data = line.slice('data:'.length).trim();
                if (!data) continue;

                let parsed;
                try {
                    parsed = JSON.parse(data);
                } catch {
                    continue; // fragmento no JSON: se ignora
                }

                if (currentEvent === 'chunk' && parsed.text) {
                    onChunk(parsed.text);
                } else if (currentEvent === 'done') {
                    quotaInfo = parsed;
                } else if (currentEvent === 'error') {
                    // Fuera del try/catch del JSON: el error SIEMPRE llega a la UI
                    reader.cancel().catch(() => {});
                    throw new Error(parsed.message || 'Error en el chat de IA');
                }
            }
        }
    }

    return quotaInfo || {};
}

/**
 * Envía un mensaje al chat de IA (sin streaming — fallback).
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