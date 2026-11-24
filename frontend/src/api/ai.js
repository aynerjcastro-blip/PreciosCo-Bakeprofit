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
            Accept: 'text/event-stream',
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

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // Procesar líneas completas del buffer
        const lines = buffer.split('\n');
        // Guardar la última línea incompleta para la siguiente iteración
        buffer = lines.pop() || '';

        let currentEvent = null;

        for (const line of lines) {
            if (line.startsWith('event:')) {
                currentEvent = line.slice('event:'.length).trim();
            } else if (line.startsWith('data:')) {
                const data = line.slice('data:'.length).trim();
                if (!data) continue;

                try {
                    const parsed = JSON.parse(data);

                    if (currentEvent === 'chunk' && parsed.text) {
                        onChunk(parsed.text);
                    } else if (currentEvent === 'done') {
                        quotaInfo = parsed;
                    } else if (currentEvent === 'error') {
                        throw new Error(parsed.message || 'Error en streaming');
                    }
                } catch (e) {
                    // Si no es JSON válido y no es un error nuestro, seguir
                    if (e.message === 'Error en streaming') throw e;
                }

                currentEvent = null; // reset después de procesar data
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