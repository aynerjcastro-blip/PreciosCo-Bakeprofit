// buscar.js
// API_BASE viene de config.js (cargado antes que este archivo en buscar.html).
const DEBOUNCE_MS = 400;

const searchInput = document.getElementById('search-input');
const categoryFilterEl = document.getElementById('category-filter');
const searchResultsEl = document.getElementById('search-results');

let categories = [];
let selectedCategoryId = null; // null = "Todas"
let debounceTimer = null;
let currentSearchController = null; // controla qué petición de búsqueda es la vigente

async function init() {
    await loadCategories();
    await loadProducts();

    searchInput.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(loadProducts, DEBOUNCE_MS);
    });
}

async function loadCategories() {
    try {
        const response = await fetch(`${API_BASE}/categories/active`);
        if (!response.ok) throw new Error('No se pudieron cargar las categorías');

        categories = await response.json();
        renderCategoryChips();
    } catch (error) {
        console.error(error);
        // Si falla el filtro de categorías, igual dejamos usable la búsqueda por nombre.
        categoryFilterEl.innerHTML = '';
    }
}

function renderCategoryChips() {
    categoryFilterEl.innerHTML = '';
    categoryFilterEl.appendChild(createCategoryChip('Todas', null));

    categories.forEach((category) => {
        categoryFilterEl.appendChild(createCategoryChip(category.name, category.id));
    });
}

function createCategoryChip(label, id) {
    const chip = document.createElement('button');
    chip.type = 'button';
    chip.className = 'category-filter__chip';
    chip.textContent = label;

    const isActive = id === selectedCategoryId;
    chip.setAttribute('aria-pressed', isActive);
    if (isActive) {
        chip.classList.add('category-filter__chip--active');
    }

    chip.addEventListener('click', () => {
        selectedCategoryId = id;
        clearTimeout(debounceTimer); // un clic en chip no debe esperar el debounce del texto
        renderCategoryChips();
        loadProducts();
    });

    return chip;
}

async function loadProducts() {
    // Cancela la búsqueda anterior si todavía estaba en vuelo, para que una
    // respuesta lenta no sobreescriba el resultado de una búsqueda más reciente.
    if (currentSearchController) {
        currentSearchController.abort();
    }
    const controller = new AbortController();
    currentSearchController = controller;

    renderState('loading', 'Buscando productos...');

    const params = new URLSearchParams();
    const name = searchInput.value.trim();
    if (name) params.set('name', name);
    if (selectedCategoryId !== null) params.set('idCategory', selectedCategoryId);

    try {
        const response = await fetch(`${API_BASE}/products/search?${params.toString()}`, {
            signal: controller.signal,
        });
        if (!response.ok) throw new Error('Error al consultar productos');

        const products = await response.json();
        renderProducts(products);
    } catch (error) {
        if (error.name === 'AbortError') return; // fue cancelada por una búsqueda más nueva, no es un error real
        console.error(error);
        renderState(
            'error',
            'No pudimos conectar con el servidor. Verifica que el backend esté corriendo e intenta de nuevo.'
        );
    }
}

function renderProducts(products) {
    if (products.length === 0) {
        renderState('empty', 'No encontramos productos con esos filtros. Prueba con otro nombre o categoría.');
        return;
    }

    searchResultsEl.innerHTML = '';
    products.forEach((product) => {
        searchResultsEl.appendChild(createProductCard(product));
    });
}

function createProductCard(product) {
    const card = document.createElement('article');
    card.className = 'product-card';

    const category = document.createElement('span');
    category.className = 'product-card__category';
    category.textContent = product.category;

    const name = document.createElement('h3');
    name.className = 'product-card__name';
    name.textContent = product.name;

    const unit = document.createElement('p');
    unit.className = 'product-card__unit';
    unit.textContent = product.unit ?? '';

    card.append(category, name, unit);
    return card;
}

function renderState(type, message) {
    const titles = {
        loading: 'Cargando',
        empty: 'Sin resultados',
        error: 'Algo salió mal',
    };

    searchResultsEl.innerHTML = '';

    const state = document.createElement('div');
    state.className = 'search-state';

    const title = document.createElement('p');
    title.className = 'search-state__title';
    title.textContent = titles[type] ?? '';

    const text = document.createElement('p');
    text.textContent = message;

    state.append(title, text);
    searchResultsEl.appendChild(state);
}

init();