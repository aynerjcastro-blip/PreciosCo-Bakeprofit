import { useEffect, useState } from 'react';
import { findActiveCategories, findActiveSections } from '../api/categories';
import { findActiveStores } from '../api/stores';

const EMPTY_CATALOG = {
    categories: [], stores: [], sectionId: null, status: 'loading',
};

function normalizeSectionName(name) {
    return name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

export default function useSearchCatalog(sectionName) {
    const [catalog, setCatalog] = useState(EMPTY_CATALOG);
    const [attempt, setAttempt] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        async function loadCatalog() {
            setCatalog({ ...EMPTY_CATALOG, scope: sectionName });
            try {
                let sectionId = null;
                if (sectionName) {
                    const sections = await findActiveSections({ signal: controller.signal });
                    if (controller.signal.aborted) return;
                    const section = sections.find((candidate) =>
                        normalizeSectionName(candidate.name) === normalizeSectionName(sectionName));
                    if (!section) {
                        setCatalog({ ...EMPTY_CATALOG, scope: sectionName, status: 'unavailable' });
                        return;
                    }
                    sectionId = section.id;
                }
                const options = { sectionId, signal: controller.signal };
                const [categories, stores] = await Promise.all([
                    findActiveCategories(options), findActiveStores(options),
                ]);
                if (!controller.signal.aborted) {
                    setCatalog({ categories, stores, sectionId, scope: sectionName, status: 'ready' });
                }
            } catch {
                if (!controller.signal.aborted) {
                    setCatalog({ ...EMPTY_CATALOG, scope: sectionName, status: 'error' });
                }
            }
        }
        loadCatalog();
        return () => controller.abort();
    }, [sectionName, attempt]);

    return {
        ...(catalog.scope === sectionName ? catalog : EMPTY_CATALOG),
        retry: () => setAttempt((currentAttempt) => currentAttempt + 1),
    };
}
