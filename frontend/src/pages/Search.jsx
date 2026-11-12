import { useState, useEffect, useRef } from 'react';
import styles from './Search.module.css';
import {
    CartIcon,
    PillIcon,
    FaceIcon,
    GearIcon,
    SearchIcon,
} from '../components/ui/icons/Sectoricons.jsx';
import { Link, useSearchParams } from 'react-router-dom';
import { searchProducts } from '../api/products';
import { findActiveStores } from '../api/stores';
import StoreFilter from '../components/search/StoreFilter/StoreFilter.jsx';
import PriceRangeFilter from '../components/search/PriceRangeFilter/PriceRangeFilter.jsx';
import SearchPagination from '../components/search/SearchPagination/SearchPagination.jsx';

const API_BASE = 'http://localhost:8080/api';

/**
 * Configuracion estatica de los sectores.
 * Se define fuera del componente para evitar recrear el arreglo en cada render.
 * El campo 'available' controla si el sector hace peticiones o muestra la pantalla "Llega pronto".
 */
const SECTORS = [
    {
        id: 'supermercados',
        label: 'Supermercados',
        subtitle: 'Compara precios y ahorra',
        Icon: CartIcon,
        available: true,
    },
    {
        id: 'droguerias',
        label: 'Droguerias',
        subtitle: 'Encuentra mejores precios',
        Icon: PillIcon,
        available: false,
    },
    {
        id: 'cuidado-facial',
        label: 'Cuidado Facial',
        subtitle: 'Encuentra el cuidado que mereces',
        Icon: FaceIcon,
        available: false,
    },
    {
        id: 'repuestos',
        label: 'Repuestos',
        subtitle: 'Encuentra la pieza que necesitas',
        Icon: GearIcon,
        available: false,
    },
];

const SAVED_KEY = 'searchState';

// Filtros de HU-06b en la URL: se pueden compartir y sobreviven a Atrás y Adelante.
// La tienda se repite para varias (?tienda=1&tienda=3); la página empieza en 1 para el usuario.
const PARAM_STORE = 'tienda';
const PARAM_MIN_PRICE = 'min';
const PARAM_MAX_PRICE = 'max';
const PARAM_PAGE = 'pagina';
const PAGE_SIZE = 20;
const STORE_SEPARATOR = ',';

/** Página de la URL; cualquier valor que no sea un entero desde 1 es la primera. */
function parsePage(value) {
    const page = Number.parseInt(value, 10);
    return Number.isInteger(page) && page >= 1 ? page : 1;
}

/** Pone el parámetro, o lo quita si el valor está vacío. */
function setOrDelete(params, key, value) {
    if (value) {
        params.set(key, value);
    } else {
        params.delete(key);
    }
}

/**
 * Recupera el estado de busqueda anterior de la sesion del navegador.
 * Por que: Mejora la experiencia del usuario al mantener sus filtros 
 * si navega a otra pagina (como el comparador) y luego regresa.
 * Se usa try/catch porque algunos navegadores bloquean sessionStorage en modo incognito.
 */
function loadSaved() {
    try {
        return JSON.parse(sessionStorage.getItem(SAVED_KEY)) || {};
    } catch {
        return {};
    }
}

const formatCop = (value) => `$${Number(value).toLocaleString('es-CO')}`;

/**
 * Ahorro posible de un producto: lo que se paga de más en la tienda más cara frente a la más barata.
 * Los campos minPrice y maxPrice los entrega el backend (HT-16: contrato del buscador); si no
 * vienen, el producto se muestra sin precio y no hay ahorro que calcular.
 */
function savingOf(product) {
    if (product.minPrice == null || product.maxPrice == null) return 0;
    return Math.max(0, Number(product.maxPrice) - Number(product.minPrice));
}

// Foto del producto; sin imagen (o si no carga) se muestra un marcador
function Thumb({ src, alt }) {
    const [failed, setFailed] = useState(false);

    if (!src || failed) {
        return <span className={styles['product-row__placeholder']} aria-hidden="true">🛒</span>;
    }
    return (
        <img
            className={styles['product-row__image']}
            src={src}
            alt={alt}
            loading="lazy"
            referrerPolicy="no-referrer"
            onError={() => setFailed(true)}
        />
    );
}

// Fila de resultado: foto | datos | precio desde | llamada a comparar
function ProductRow({ product }) {
    const hasPrice = product.minPrice != null;
    const saving = savingOf(product);

    return (
        <Link to={`/comparador?productId=${product.id}`} className={styles['product-row']}>
            <div className={styles['product-row__media']}>
                <Thumb src={product.imageUrl} alt={product.name} />
            </div>

            <div className={styles['product-row__info']}>
                <p className={styles['product-row__tags']}>
                    <span className={styles['product-row__category']}>{product.category}</span>
                    {product.brand && <span className={styles['product-row__brand']}>{product.brand}</span>}
                    {product.unit && <span className={styles['product-row__unit']}>{product.unit}</span>}
                </p>
                <h3 className={styles['product-row__name']}>{product.name}</h3>
                {hasPrice && product.minPriceStoreName && (
                    <p className={styles['product-row__best']}>
                        Más barato en <strong>{product.minPriceStoreName}</strong>
                    </p>
                )}
            </div>

            <div className={styles['product-row__price']}>
                {hasPrice ? (
                    <>
                        <span className={styles['product-row__from']}>Desde</span>
                        <span className={styles['product-row__value']}>{formatCop(product.minPrice)}</span>
                        {saving > 0 && (
                            <span className={styles['product-row__saving']}>Ahorras hasta {formatCop(saving)}</span>
                        )}
                        {product.storeCount > 0 && (
                            <span className={styles['product-row__stores']}>
                                {product.storeCount === 1 ? 'En 1 tienda' : `En ${product.storeCount} tiendas`}
                            </span>
                        )}
                    </>
                ) : (
                    <span className={styles['product-row__stores']}>Sin precio vigente</span>
                )}
            </div>

            <span className={styles['product-row__cta']}>Comparar precios <span aria-hidden="true">›</span></span>
        </Link>
    );
}

/** Destaca, entre los resultados de la página, el producto con mayor diferencia entre tiendas. */
function SavingsBanner({ products }) {
    let top = null;
    for (const product of products) {
        if (savingOf(product) > (top ? savingOf(top) : 0)) top = product;
    }
    if (!top) return null;

    const saving = savingOf(top);
    const percent = Math.round((saving / Number(top.maxPrice)) * 100);

    return (
        <Link to={`/comparador?productId=${top.id}`} className={styles['savings-banner']}>
            <span className={styles['savings-banner__tag']}>Mayor ahorro en esta página · {percent} % menos</span>
            <span className={styles['savings-banner__text']}>
                {top.name}: ahorras hasta <strong>{formatCop(saving)}</strong>
                {top.minPriceStoreName ? <> comprando en {top.minPriceStoreName}</> : null}
            </span>
        </Link>
    );
}

export default function Search() {
    const saved = loadSaved();
    const [searchParams, setSearchParams] = useSearchParams();

    // 1. FUENTES DE VERDAD EXTERNAS (URL)
    // Extraemos parametros de la URL para habilitar navegacion directa (ej. links desde el Header).
    const urlQuery = searchParams.get('q');
    const urlSector = searchParams.get('sector');

    // Filtros de tienda, precio y página: la URL es su única fuente de verdad (sin estado local)
    const selectedStoreIds = searchParams.getAll(PARAM_STORE);
    const storeKey = selectedStoreIds.join(STORE_SEPARATOR);
    const minPrice = searchParams.get(PARAM_MIN_PRICE) ?? '';
    const maxPrice = searchParams.get(PARAM_MAX_PRICE) ?? '';
    const pageNumber = parsePage(searchParams.get(PARAM_PAGE));

    // 2. ESTADOS LOCALES
    // Inicializacion inteligente: La prioridad es 1. URL, 2. Sesion guardada, 3. Valor por defecto.
    // Por que: Garantiza que un link externo siempre sobreescriba una busqueda vieja guardada.
    const [searchTerm, setSearchTerm] = useState(urlQuery ?? saved.searchTerm ?? '');
    const [selectedSectorId, setSelectedSectorId] = useState(urlSector ?? saved.selectedSectorId ?? null);

    const [categories, setCategories] = useState([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState(saved.selectedCategoryId ?? null);
    const [showAll, setShowAll] = useState(saved.showAll ?? false);

    const [stores, setStores] = useState([]);
    // Al cambiar de pagina se vuelve al inicio de los filtros, no al inicio de la pagina
    const refineRef = useRef(null);

    const [fetchedProducts, setFetchedProducts] = useState([]);
    const [pageInfo, setPageInfo] = useState({ totalElements: 0, totalPages: 0 });
    // 'idle' = inactivo, 'loading' = cargando, 'empty' = vacio, 'error' = fallo de red, 'success' = exito,
    // 'stores-unavailable' = una tienda de la URL ya no existe o se desactivo (404),
    // 'invalid-filters' = el backend rechazo el rango de precio (400)
    const [fetchStatus, setFetchStatus] = useState('idle');
    // Solo en pantallas angostas: el panel de filtros se abre y se cierra
    const [filtersOpen, setFiltersOpen] = useState(false);

    // =========================================================================
    // SINCRONIZACION CON LA URL
    // =========================================================================
    // Por que: Si el usuario usa los botones "Atras" o "Adelante" del navegador, 
    // la URL cambiara, pero el componente no se recargara. Este efecto detecta esos 
    // cambios y actualiza el estado local para que la UI refleje la URL.
    const [prevUrlQuery, setPrevUrlQuery] = useState(urlQuery);
    const [prevUrlSector, setPrevUrlSector] = useState(urlSector);

    if (urlQuery !== prevUrlQuery) {
        setPrevUrlQuery(urlQuery);
        setSearchTerm(urlQuery ?? '');
    }

    if (urlSector !== prevUrlSector) {
        setPrevUrlSector(urlSector);
        setSelectedSectorId(urlSector ?? null);
    }

    /**
     * Cambia los filtros de la URL y vuelve a la primera pagina: con otros filtros,
     * la pagina en la que estaba el usuario ya no corresponde a los mismos resultados.
     */
    function changeFilters(mutate) {
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            mutate(next);
            next.delete(PARAM_PAGE);
            return next;
        });
    }

    /** Vuelve a la primera pagina cuando cambia un filtro que no vive en la URL (texto, categoria, sector). */
    function backToFirstPage() {
        if (searchParams.has(PARAM_PAGE)) changeFilters(() => { });
    }

    /**
     * Maneja el cambio de texto en el input de busqueda.
     * Por que: Actualiza el estado local inmediatamente, pero quita '?q=' de la URL.
     * Si no lo quitaramos, el estado local chocaria con el parametro estatico anterior.
     * Solo se quitan 'q' y la pagina: los filtros de tienda y precio se conservan.
     */
    const handleSearchChange = (e) => {
        const newValue = e.target.value;
        setSearchTerm(newValue);

        if (urlQuery || searchParams.has(PARAM_PAGE)) {
            changeFilters((next) => next.delete('q'));
        }
    };

    // =========================================================================
    // VARIABLES DERIVADAS
    // =========================================================================
    // Por que: Se calculan en cada render en lugar de usar estados adicionales 
    // para evitar problemas de sincronizacion ("React anti-pattern: derived state").
    const selectedSector = SECTORS.find((s) => s.id === selectedSectorId) || null;
    const hasTerm = searchTerm.trim() !== '';
    const hasPriceFilter = storeKey !== '' || minPrice !== '' || maxPrice !== '';
    const hasFilter = selectedCategoryId !== null || selectedSectorId !== null || showAll || hasPriceFilter;

    // isIdle: Evita hacer peticiones a la API cuando el usuario no ha pedido nada.
    const isIdle = !hasTerm && !hasFilter;
    // isSoon: Intercepta sectores en desarrollo para no enviar peticiones basura al backend.
    const isSoon = !isIdle && selectedSector !== null && !selectedSector.available;

    const status = isIdle ? 'idle' : isSoon ? 'soon' : fetchStatus;
    const products = isIdle || isSoon ? [] : fetchedProducts;

    // =========================================================================
    // PERSISTENCIA DE SESION
    // =========================================================================
    // Por que: Guarda silenciosamente los filtros activos. Si el usuario hace clic 
    // en un producto y luego decide volver atras, la pagina estara exactamente como la dejo.
    useEffect(() => {
        sessionStorage.setItem(
            SAVED_KEY,
            JSON.stringify({ searchTerm, selectedCategoryId, selectedSectorId, showAll })
        );
    }, [searchTerm, selectedCategoryId, selectedSectorId, showAll]);

    // =========================================================================
    // CARGA INICIAL: CATEGORIAS
    // =========================================================================
    useEffect(() => {
        async function fetchCategories() {
            try {
                const response = await fetch(`${API_BASE}/categories/active`);
                if (!response.ok) throw new Error('Error al cargar categorias');
                const data = await response.json();
                setCategories(data);
            } catch (error) {
                console.error(error);
                // Por que: Falla silenciosamente porque la app aun es usable mediante
                // busqueda por texto libre, no queremos bloquear al usuario entero.
            }
        }
        fetchCategories();
    }, []);

    // =========================================================================
    // CARGA INICIAL: TIENDAS ACTIVAS (selector de HU-06b)
    // =========================================================================
    useEffect(() => {
        findActiveStores()
            .then(setStores)
            .catch((error) => {
                // Igual que las categorias: sin tiendas la busqueda sigue funcionando
                console.error(error);
            });
    }, []);

    // =========================================================================
    // MOTOR DE BUSQUEDA (DEBOUNCE + ABORT CONTROLLER)
    // =========================================================================
    useEffect(() => {
        if (isIdle || isSoon) return;

        // Por que AbortController: Si el usuario escribe "caf" y luego "cafe" rapidamente, 
        // habra 2 peticiones en vuelo. Si "caf" responde mas lento que "cafe", los 
        // resultados en pantalla seran incorrectos (Condicion de carrera). 
        // AbortController cancela peticiones antiguas instantaneamente.
        const controller = new AbortController();

        const fetchProducts = async () => {
            setFetchStatus('loading');
            // Se reconstruye desde storeKey: un arreglo nuevo en cada render dispararia el efecto sin fin
            const storeIds = storeKey ? storeKey.split(STORE_SEPARATOR) : [];

            try {
                const result = await searchProducts(
                    {
                        name: hasTerm ? searchTerm.trim() : undefined,
                        categoryId: selectedCategoryId,
                        storeIds,
                        minPrice,
                        maxPrice,
                        page: pageNumber - 1,
                        size: PAGE_SIZE,
                    },
                    { signal: controller.signal },
                );
                setFetchedProducts(result.content);
                setPageInfo({
                    totalElements: result.page.totalElements,
                    totalPages: result.page.totalPages,
                });
                setFetchStatus(result.content.length === 0 ? 'empty' : 'success');
            } catch (error) {
                // Se ignora el AbortError porque es el comportamiento esperado al seguir escribiendo
                if (error.name === 'AbortError') return;
                console.error(error);
                if (error.status === 404 && storeIds.length > 0) {
                    setFetchStatus('stores-unavailable');
                } else if (error.status === 400) {
                    setFetchStatus('invalid-filters');
                } else {
                    setFetchStatus('error');
                }
            }
        };

        // Por que Debounce (400ms): Evita que se dispare una llamada a la base de datos 
        // por cada letra que el usuario teclea, reduciendo enormemente los costos del servidor.
        const debounceTimer = setTimeout(fetchProducts, 400);

        // Funcion de limpieza: Se ejecuta si las dependencias cambian ANTES de los 400ms,
        // cancelando el timer actual y la peticion HTTP en vuelo.
        return () => {
            clearTimeout(debounceTimer);
            controller.abort();
        };
    }, [isIdle, isSoon, hasTerm, searchTerm, selectedCategoryId, storeKey, minPrice, maxPrice, pageNumber]);

    // =========================================================================
    // MANEJADORES DE UI
    // =========================================================================
    function showAllProducts() {
        setSelectedCategoryId(null);
        setSelectedSectorId(null);
        setShowAll(true);
        backToFirstPage();
    }

    function toggleSector(id) {
        setShowAll(false);
        setSelectedSectorId((current) => (current === id ? null : id));
        backToFirstPage();
    }

    function toggleCategory(id) {
        setShowAll(false);
        setSelectedCategoryId(selectedCategoryId === id ? null : id);
        backToFirstPage();
    }

    function toggleStore(id) {
        changeFilters((next) => {
            const current = next.getAll(PARAM_STORE);
            const updated = current.includes(id)
                ? current.filter((storeId) => storeId !== id)
                : [...current, id];
            next.delete(PARAM_STORE);
            updated.forEach((storeId) => next.append(PARAM_STORE, storeId));
        });
    }

    function clearStores() {
        changeFilters((next) => next.delete(PARAM_STORE));
    }

    function applyPriceRange(min, max) {
        changeFilters((next) => {
            setOrDelete(next, PARAM_MIN_PRICE, min);
            setOrDelete(next, PARAM_MAX_PRICE, max);
        });
    }

    function goToPage(page) {
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            setOrDelete(next, PARAM_PAGE, page > 1 ? String(page) : '');
            return next;
        });
        refineRef.current?.scrollIntoView({ block: 'start' });
    }

    const activeFilterCount =
        (selectedCategoryId !== null ? 1 : 0) + selectedStoreIds.length + (minPrice !== '' || maxPrice !== '' ? 1 : 0);

    // "Limpiar todo": quita categoría, tiendas y precio; el sector es navegación y se conserva
    function clearAllFilters() {
        setSelectedCategoryId(null);
        setShowAll(false);
        changeFilters((next) => {
            next.delete(PARAM_STORE);
            next.delete(PARAM_MIN_PRICE);
            next.delete(PARAM_MAX_PRICE);
        });
    }

    const noFilters = selectedCategoryId === null && selectedSectorId === null;

    return (
        <main className={styles['search-page']}>
            <div className={styles['search-page__container']}>
                <header className={styles['search-head']}>
                    <h1 className={styles['search-head__title']}>Busca tu producto</h1>
                    <p className={styles['search-head__subtitle']}>
                        Compara el precio en cada tienda y compra donde más ahorras
                    </p>
                </header>

                {/* BARRA DE BUSQUEDA */}
                <div className={styles['search-bar']}>
                    <span className={styles['search-bar__icon']}>
                        <SearchIcon />
                    </span>
                    <input
                        type="search"
                        className={styles['search-bar__input']}
                        placeholder="Ej. Arroz, Leche, Chocolatina..."
                        value={searchTerm}
                        onChange={handleSearchChange}
                        aria-label="Buscar producto por nombre"
                    />
                </div>

                {/* SECTORES: pestañas compactas, ya no tarjetas que empujan los resultados fuera de la pantalla */}
                <div className={styles['sector-tabs']} role="group" aria-label="Sector">
                    {SECTORS.map((sector) => {
                        const active = selectedSectorId === sector.id;
                        return (
                            <button
                                key={sector.id}
                                type="button"
                                className={`${styles['sector-tab']} ${active ? styles['sector-tab--active'] : ''}`}
                                onClick={() => toggleSector(sector.id)}
                                aria-pressed={active}
                                title={sector.subtitle}
                            >
                                <sector.Icon />
                                {sector.label}
                                {!sector.available && <span className={styles['sector-tab__soon']}>Pronto</span>}
                            </button>
                        );
                    })}
                </div>

                <div className={styles['search-layout']}>
                    {/* PANEL DE FILTROS */}
                    <aside className={styles.filters} aria-label="Filtros">
                        <button
                            type="button"
                            className={styles['filters__toggle']}
                            aria-expanded={filtersOpen}
                            aria-controls="panel-filtros"
                            onClick={() => setFiltersOpen((open) => !open)}
                        >
                            Filtros{activeFilterCount > 0 ? ` (${activeFilterCount})` : ''}
                        </button>

                        <div
                            id="panel-filtros"
                            className={`${styles['filters__panel']} ${filtersOpen ? styles['filters__panel--open'] : ''}`}
                        >
                            <div className={styles['filters__head']}>
                                <h2 className={styles['filters__title']}>Filtros</h2>
                                {activeFilterCount > 0 && (
                                    <button type="button" className={styles['filters__clear']} onClick={clearAllFilters}>
                                        Limpiar todo
                                    </button>
                                )}
                            </div>

                            {categories.length > 0 && (
                                <div className={styles['filter-card']} role="radiogroup" aria-labelledby="categoria-label">
                                    <span id="categoria-label" className={styles['filter-card__title']}>Categoría</span>
                                    <label className={styles['filter-option']}>
                                        <input
                                            type="radio"
                                            name="categoria"
                                            checked={noFilters}
                                            onChange={showAllProducts}
                                        />
                                        <span>Todas</span>
                                    </label>
                                    {categories.map((cat) => (
                                        <label key={cat.id} className={styles['filter-option']}>
                                            <input
                                                type="radio"
                                                name="categoria"
                                                checked={selectedCategoryId === cat.id}
                                                onChange={() => toggleCategory(cat.id)}
                                            />
                                            <span>{cat.name}</span>
                                        </label>
                                    ))}
                                </div>
                            )}

                            {/* HU-06b: tienda y precio se resuelven en el backend */}
                            <div className={styles['filter-card']}>
                                <StoreFilter
                                    stores={stores}
                                    selectedIds={selectedStoreIds}
                                    onToggle={toggleStore}
                                    onClear={clearStores}
                                />
                            </div>
                            <div className={styles['filter-card']}>
                                <PriceRangeFilter min={minPrice} max={maxPrice} onApply={applyPriceRange} />
                            </div>
                        </div>
                    </aside>

                    {/* RESULTADOS */}
                    <section ref={refineRef} className={styles['search-main']} aria-label="Resultados">
                        {status === 'success' && (
                            <p className={styles['search-count']}>
                                {pageInfo.totalElements === 1 ? '1 producto' : `${pageInfo.totalElements} productos`}
                                {hasTerm && <> para <strong>“{searchTerm.trim()}”</strong></>}
                            </p>
                        )}

                        {status === 'success' && <SavingsBanner products={products} />}

                        {/* aria-live="polite" notifica a lectores de pantalla los cambios sin interrumpir */}
                        <div className={styles['search-results']} aria-live="polite">
                            {status === 'idle' && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Empieza tu búsqueda</p>
                                    <p>Escribe un producto, elige una categoría o una tienda.</p>
                                    <button type="button" className={styles['search-state__action']} onClick={showAllProducts}>
                                        Ver todos los productos
                                    </button>
                                </div>
                            )}

                            {status === 'loading' && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Cargando</p>
                                    <p>Buscando productos...</p>
                                </div>
                            )}

                            {status === 'soon' && selectedSector && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>
                                        {selectedSector.label} llega pronto
                                    </p>
                                    <p>Todavía no tenemos precios de este sector. Mientras tanto, prueba con Supermercados.</p>
                                </div>
                            )}

                            {status === 'empty' && pageNumber > 1 && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Esta página ya no tiene resultados</p>
                                    <p>Los resultados cambiaron desde que se abrió este enlace.</p>
                                    <button type="button" className={styles['search-state__action']} onClick={() => goToPage(1)}>
                                        Ir a la primera página
                                    </button>
                                </div>
                            )}

                            {status === 'empty' && pageNumber <= 1 && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Sin resultados</p>
                                    <p>No encontramos productos con esos filtros. Prueba con otro nombre, categoría, tienda o precio.</p>
                                </div>
                            )}

                            {status === 'stores-unavailable' && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Una de las tiendas ya no está disponible</p>
                                    <p>Quita el filtro de tienda para ver los resultados de las demás.</p>
                                    <button type="button" className={styles['search-state__action']} onClick={clearStores}>
                                        Quitar filtro de tienda
                                    </button>
                                </div>
                            )}

                            {status === 'invalid-filters' && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Revisa el rango de precio</p>
                                    <p>El mínimo no puede ser mayor que el máximo, y ninguno puede ser negativo.</p>
                                    <button
                                        type="button"
                                        className={styles['search-state__action']}
                                        onClick={() => applyPriceRange('', '')}
                                    >
                                        Quitar precio
                                    </button>
                                </div>
                            )}

                            {status === 'error' && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Algo salió mal</p>
                                    <p>No pudimos conectar con el servidor. Verifica tu conexión.</p>
                                </div>
                            )}

                            {status === 'success' && products.map((product) => (
                                <ProductRow key={product.id} product={product} />
                            ))}
                        </div>

                        {status === 'success' && (
                            <SearchPagination page={pageNumber} totalPages={pageInfo.totalPages} onChange={goToPage} />
                        )}
                    </section>
                </div>
            </div>
        </main>
    );
}