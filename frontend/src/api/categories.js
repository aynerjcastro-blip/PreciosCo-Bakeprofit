const API_BASE = 'http://localhost:8080/api/categories';

async function getCategories(path, signal) {
    const response = await fetch(`${API_BASE}${path}`, { signal });
    if (!response.ok) {
        const error = new Error('No pudimos cargar las categorías de esta sección.');
        error.status = response.status;
        throw error;
    }
    return response.json();
}

export function findActiveSections({ signal } = {}) {
    return getCategories('/roots', signal);
}

export function findActiveCategories({ sectionId, signal } = {}) {
    const query = new URLSearchParams();
    if (sectionId != null) query.set('sectionId', sectionId);
    return getCategories(`/active?${query}`, signal);
}
