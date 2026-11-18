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
import useSearchCatalog from '../hooks/useSearchCatalog';
import StoreFilter from '../components/search/StoreFilter/StoreFilter.jsx';
import PriceRangeFilter from '../components/search/PriceRangeFilter/PriceRangeFilter.jsx';
import SearchPagination from '../components/search/SearchPagination/SearchPagination.jsx';


/**
 * Sectores que muestran las pestañas. Se define fuera del componente para no recrear el
 * arreglo en cada render.
 *
 * IMPORTANTE: el `id` de cada sector es un slug que también existe en config/sections.js, y es
 * el mismo que viaja en la URL (?sector=supermercados). Si un slug cambia en un archivo y no
 * en el otro, useSectionBySlug no encuentra la sección y el sector queda como "llega pronto".
 * Aquí viven solo los datos de presentación (ícono, textos); el id numérico de la sección en
 * la base de datos NO se escribe aquí: se obtiene del backend con useSectionBySlug.
 *
 * `available` es una guarda optimista: evita pedir datos de un sector que sabemos que no existe
 * todavía. La verdad la decide el backend (ver `sectionUnavailable` más abajo): si una sección
 * marcada como disponible llega sin productos, también se trata como "llega pronto".
 */
const SECTORS = [
    {
        id: 'supermercados',
        rootName: 'Supermercado',
        label: 'Supermercados',
        subtitle: 'Compara precios y ahorra',
        Icon: CartIcon,
        available: true,
    },
    {
        id: 'droguerias',
        rootName: 'Droguería',
        label: 'Droguerías',
        subtitle: 'Encuentra mejores precios',
        Icon: PillIcon,
        available: true,
    },
    {
        id: 'cuidado-facial',
        rootName: 'Cuidado facial',
        label: 'Cuidado Facial',
        subtitle: 'Encuentra el cuidado que mereces',
        Icon: FaceIcon,
        available: true,
    },
    {
        id: 'repuestos',
        rootName: 'Repuestos',
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
const SEARCH_DEBOUNCE_MS = 400;

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
 * Ojo: lo guardado puede estar desactualizado (por ejemplo, un sector que ya no existe),
 * por eso el componente nunca lo da por válido sin comprobarlo (ver `knownSectorId`).
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
 * Es la diferencia absoluta en pesos; el backend ordena por el porcentaje (DP-39), así que lo
 * que se muestra aquí y el orden de la lista no siempre coinciden en cuál producto ahorra "más".
 */
function savingOf(product) {
    if (product.minPrice == null || product.maxPrice == null) return 0;
    return Math.max(0, Number(product.maxPrice) - Number(product.minPrice));
}

// Foto del producto; sin imagen (o si no carga) se muestra un marcador
function Thumb({ src, alt }) {
    const [failed, setFailed] = useState(false);

    if (!src || failed) {
        return <span className={styles['product-card__placeholder']} aria-hidden="true">🛒</span>;
    }
    return (
        <img
            className={styles['product-card__image']}
            src={src}
            alt={alt}
            loading="lazy"
            referrerPolicy="no-referrer"
            onError={() => setFailed(true)}
        />
    );
}

// Cuántas tiendas se ven de entrada; el resto se despliega con "Ver N tiendas más"
const VISIBLE_OFFERS = 2;

// Fecha corta de la última confirmación del precio (RF10); sin fecha válida no se muestra
function formatShortDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
}

function ExternalLinkIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M15 3h6v6" />
            <path d="M10 14 21 3" />
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
        </svg>
    );
}

function ChevronIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="m6 9 6 6 6-6" />
        </svg>
    );
}

/**
 * Una tienda con su precio vigente. Si el producto está en varias tiendas, la más barata (o las
 * empatadas en lo más barato) se resalta, y las demás muestran cuánto cuestan de más.
 * El enlace a la tienda (RF57) solo existe si el scraper registró la página: nunca se muestra roto.
 */
function OfferRow({ offer, cheapestValue, compare, entering }) {
    const value = Number(offer.value);
    const isCheapest = compare && value === cheapestValue;
    const extra = compare ? value - cheapestValue : 0;
    const confirmedOn = offer.lastSeenAt ? formatShortDate(offer.lastSeenAt) : '';

    return (
        <li className={[
            styles.offer,
            isCheapest ? styles['offer--best'] : '',
            entering ? styles['offer--enter'] : '',
        ].join(' ')}>
            <StoreLogo name={offer.storeName} />

            <div className={styles.offer__store}>
                <p className={styles.offer__name}>
                    {offer.storeName}
                    {isCheapest && <span className={styles.offer__chip}>Más barato</span>}
                </p>
                {confirmedOn && <p className={styles.offer__date}>Precio confirmado el {confirmedOn}</p>}
            </div>

            <div className={styles.offer__price}>
                <span className={styles.offer__value}>{formatCop(value)}</span>
                {extra > 0 && <span className={styles.offer__extra}>+{formatCop(extra)} más</span>}
            </div>

            {offer.productUrl ? (
                <a
                    className={styles.offer__link}
                    href={offer.productUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={`Ir a ${offer.storeName} (se abre en una pestaña nueva)`}
                >
                    <ExternalLinkIcon />
                </a>
            ) : (
                <span className={styles['offer__link-slot']} aria-hidden="true" />
            )}
        </li>
    );
}

/**
 * Tarjeta de resultado: foto y nombre arriba, y debajo una fila por tienda (las 2 más baratas;
 * el resto se despliega). El título es el enlace principal al comparador y "Comparar precios"
 * es un enlace aparte: no se anidan enlaces dentro de otro enlace.
 */
function ProductCard({ product, index = 0 }) {
    const [expanded, setExpanded] = useState(false);

    const offers = product.offers ?? [];
    const hasPrice = offers.length > 0;
    const compare = offers.length > 1;
    const saving = savingOf(product);
    const maxPrice = Number(product.maxPrice);
    const savingPercent = maxPrice > 0 ? Math.round((saving / maxPrice) * 100) : 0;
    const hiddenCount = offers.length - VISIBLE_OFFERS;
    const visibleOffers = expanded ? offers : offers.slice(0, VISIBLE_OFFERS);
    const listId = `offers-${product.id}`;

    return (
        <article
            className={styles['product-card']}
            // Retraso de la animación de entrada: cada tarjeta entra un poco después de la anterior
            style={{ '--row-index': Math.min(index, 8) }}
        >
            <header className={styles['product-card__head']}>
                <div className={styles['product-card__media']}>
                    <Thumb src={product.imageUrl} alt="" />
                </div>

                <div className={styles['product-card__info']}>
                    <h3 className={styles['product-card__name']}>
                        <Link to={`/comparador?productId=${product.id}`} className={styles['product-card__name-link']}>
                            {product.name}
                        </Link>
                    </h3>
                    <p className={styles['product-card__tags']}>
                        <span className={styles['product-card__category']}>{product.category}</span>
                        {product.brand && <span className={styles['product-card__brand']}>{product.brand}</span>}
                        {product.unit && <span className={styles['product-card__unit']}>{product.unit}</span>}
                    </p>
                    {hasPrice && (
                        <p className={styles['product-card__chips']}>
                            <span className={styles.chip}>
                                {offers.length === 1 ? '1 tienda' : `${offers.length} tiendas`}
                            </span>
                            {compare && saving > 0 && (
                                <span className={`${styles.chip} ${styles['chip--saving']}`}>
                                    Ahorras hasta {formatCop(saving)} ({savingPercent}%)
                                </span>
                            )}
                            {compare && saving === 0 && (
                                <span className={styles.chip}>Mismo precio en todas</span>
                            )}
                        </p>
                    )}
                </div>

                <Link
                    to={`/comparador?productId=${product.id}`}
                    className={styles['product-card__cta']}
                    aria-label={`Comparar precios de ${product.name}`}
                >
                    Comparar precios <span aria-hidden="true">›</span>
                </Link>
            </header>

            {hasPrice ? (
                <ul id={listId} className={styles.offers}>
                    {visibleOffers.map((offer, position) => (
                        <OfferRow
                            key={offer.storeId}
                            offer={offer}
                            cheapestValue={Number(product.minPrice)}
                            compare={compare}
                            entering={expanded && position >= VISIBLE_OFFERS}
                        />
                    ))}
                </ul>
            ) : (
                <p className={styles['product-card__empty']}>Aún sin precios</p>
            )}

            {hiddenCount > 0 && (
                <button
                    type="button"
                    className={`${styles.more} ${expanded ? styles['more--open'] : ''}`}
                    aria-expanded={expanded}
                    aria-controls={listId}
                    onClick={() => setExpanded((open) => !open)}
                >
                    {expanded ? 'Ver menos' : `Ver ${hiddenCount} ${hiddenCount === 1 ? 'tienda más' : 'tiendas más'}`}
                    <ChevronIcon />
                </button>
            )}
        </article>
    );
}

// Filas de relleno que ocupan el lugar de los resultados mientras se piden: la página no salta
// al llegar la respuesta y se ve que algo está pasando.
const SKELETON_ROWS = 6;

function ResultsSkeleton() {
    return (
        <div className={styles['skeleton-list']} role="status" aria-label="Buscando productos">
            <span className={styles['visually-hidden']}>Buscando productos...</span>
            {Array.from({ length: SKELETON_ROWS }, (_, index) => (
                <div key={index} className={styles['skeleton-row']} aria-hidden="true">
                    <div className={styles['skeleton-row__head']}>
                        <span className={`${styles.skeleton} ${styles['skeleton-row__media']}`} />
                        <div className={styles['skeleton-row__info']}>
                            <span className={`${styles.skeleton} ${styles['skeleton-row__name']}`} />
                            <span className={`${styles.skeleton} ${styles['skeleton-row__tags']}`} />
                        </div>
                    </div>
                    <span className={`${styles.skeleton} ${styles['skeleton-row__offer']}`} />
                    <span className={`${styles.skeleton} ${styles['skeleton-row__offer']}`} />
                </div>
            ))}
        </div>
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

    // =========================================================================
    // 1. FUENTES DE VERDAD EXTERNAS (URL)
    // =========================================================================
    // Extraemos parametros de la URL para habilitar navegacion directa (ej. links desde el Header
    // o el boton "Buscar y comparar" de la pagina de una seccion, que abre /buscar?sector=<slug>).
    const urlQuery = searchParams.get('q');
    const urlSector = searchParams.get('sector');

    // Filtros de tienda, precio y página: la URL es su única fuente de verdad (sin estado local)
    const selectedStoreIds = searchParams.getAll(PARAM_STORE);
    const storeKey = selectedStoreIds.join(STORE_SEPARATOR);
    const minPrice = searchParams.get(PARAM_MIN_PRICE) ?? '';
    const maxPrice = searchParams.get(PARAM_MAX_PRICE) ?? '';
    const pageNumber = parsePage(searchParams.get(PARAM_PAGE));

    // =========================================================================
    // 2. ESTADOS LOCALES
    // =========================================================================
    // Inicializacion inteligente: La prioridad es 1. URL, 2. Sesion guardada, 3. Valor por defecto.
    // Por que: Garantiza que un link externo siempre sobreescriba una busqueda vieja guardada.
    const [searchTerm, setSearchTerm] = useState(urlQuery ?? saved.searchTerm ?? '');
    const [selectedSectorId, setSelectedSectorId] = useState(urlSector ?? saved.selectedSectorId ?? null);

    const [selectedCategoryId, setSelectedCategoryId] = useState(saved.selectedCategoryId ?? null);
    const [showAll, setShowAll] = useState(saved.showAll ?? false);

    // Al cambiar de pagina se vuelve al inicio de los filtros, no al inicio de la pagina
    const refineRef = useRef(null);
    const searchInputRef = useRef(null);
    const [isComposing, setIsComposing] = useState(false);

    const [fetchedProducts, setFetchedProducts] = useState([]);
    const [pageInfo, setPageInfo] = useState({ totalElements: 0, totalPages: 0 });
    // 'idle' = inactivo, 'loading' = cargando, 'empty' = vacio, 'error' = fallo de red, 'success' = exito,
    // 'stores-unavailable' = una tienda de la URL ya no existe o se desactivo (404),
    // 'invalid-filters' = el backend rechazo el rango de precio (400)
    const [fetchStatus, setFetchStatus] = useState('idle');
    const [resultScope, setResultScope] = useState(null);
    // Solo en pantallas angostas: el panel de filtros se abre y se cierra
    const [filtersOpen, setFiltersOpen] = useState(false);

    // =========================================================================
    // SINCRONIZACION CON LA URL
    // =========================================================================
    // Por que: Si el usuario usa los botones "Atras" o "Adelante" del navegador,
    // la URL cambiara, pero el componente no se recargara. Estos bloques detectan esos
    // cambios y actualizan el estado local para que la UI refleje la URL.
    //
    // Se hace durante el render (comparando con el valor anterior) y no en un useEffect:
    // es el patrón que React documenta para ajustar estado cuando cambia una prop o la URL.
    // Evita un render intermedio con datos viejos y cumple la regla de lint que prohíbe llamar
    // a setState de forma síncrona dentro de un efecto.
    const [prevUrlQuery, setPrevUrlQuery] = useState(urlQuery);
    const [prevUrlSector, setPrevUrlSector] = useState(urlSector);

    if (urlQuery !== prevUrlQuery) {
        setPrevUrlQuery(urlQuery);
        setSearchTerm(urlQuery ?? '');
    }

    if (urlSector !== prevUrlSector) {
        setPrevUrlSector(urlSector);
        setSelectedSectorId(urlSector ?? null);
        setSelectedCategoryId(null);
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
            if (urlQuery) setPrevUrlQuery(null);
            changeFilters((next) => next.delete('q'));
        }
    };

    // =========================================================================
    // VARIABLES DERIVADAS
    // =========================================================================
    // Por que: Se calculan en cada render en lugar de usar estados adicionales
    // para evitar problemas de sincronizacion ("React anti-pattern: derived state").
    const selectedSector = SECTORS.find((sector) => sector.id === selectedSectorId) || null;
    const catalog = useSearchCatalog(selectedSector?.rootName ?? selectedSectorId ?? null);
    const { categories, stores, sectionId } = catalog;
    const catalogReady = catalog.status === 'ready';
    const categoryId = categories.some((category) => category.id === selectedCategoryId)
        ? selectedCategoryId : null;
    const validStoreKey = selectedStoreIds
        .filter((selectedId) => stores.some((store) => String(store.id) === selectedId))
        .join(STORE_SEPARATOR);
    const requestScope = JSON.stringify([
        selectedSectorId, sectionId, categoryId, validStoreKey, searchTerm,
        minPrice, maxPrice, pageNumber,
    ]);
    const hasTerm = searchTerm.trim() !== '';
    const hasPriceFilter = storeKey !== '' || minPrice !== '' || maxPrice !== '';
    // Se usa knownSectorId (no selectedSectorId): un sector invalido no cuenta como filtro.
    const hasFilter = selectedCategoryId !== null || knownSectorId !== null || showAll || hasPriceFilter;

    // isIdle: Evita hacer peticiones a la API cuando el usuario no ha pedido nada.
    const isIdle = !hasTerm && !hasFilter;
    // isSoon: Intercepta sectores en desarrollo para no enviar peticiones basura al backend.
    const isSoon = !isIdle && selectedSector !== null
        && (!selectedSector.available || catalog.status === 'unavailable');

    const status = isIdle ? 'idle' : isSoon ? 'soon'
        : catalog.status === 'unavailable' ? 'unknown-section'
        : catalog.status === 'error' ? 'filters-error'
        : !catalogReady || resultScope !== requestScope ? 'loading' : fetchStatus;
    const products = status === 'success' ? fetchedProducts : [];

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

    // Una URL de otra sección no debe mantener tiendas que ya no pertenecen al filtro.
    useEffect(() => {
        if (!catalogReady || storeKey === validStoreKey) return;
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            next.delete(PARAM_STORE);
            validStoreKey.split(STORE_SEPARATOR).filter(Boolean)
                .forEach((storeId) => next.append(PARAM_STORE, storeId));
            next.delete(PARAM_PAGE);
            return next;
        }, { replace: true });
    }, [catalogReady, storeKey, validStoreKey, setSearchParams]);

    // =========================================================================
    // MOTOR DE BUSQUEDA (DEBOUNCE + ABORT CONTROLLER)
    // =========================================================================
    useEffect(() => {
        if (isIdle || isSoon || isComposing || !catalogReady || storeKey !== validStoreKey) return;

        // Por que AbortController: Si el usuario escribe "caf" y luego "cafe" rapidamente,
        // habra 2 peticiones en vuelo. Si "caf" responde mas lento que "cafe", los
        // resultados en pantalla seran incorrectos (Condicion de carrera).
        // AbortController cancela peticiones antiguas instantaneamente.
        const controller = new AbortController();

        const fetchProducts = async () => {
            setFetchStatus('loading');
            // Se reconstruye desde storeKey: un arreglo nuevo en cada render dispararia el efecto sin fin
            const storeIds = validStoreKey ? validStoreKey.split(STORE_SEPARATOR) : [];

            try {
                const result = await searchProducts(
                    {
                        name: hasTerm ? searchTerm.trim() : undefined,
                        categoryId,
                        sectionId,
                        storeIds,
                        minPrice,
                        maxPrice,
                        page: pageNumber - 1,
                        size: PAGE_SIZE,
                    },
                    { signal: controller.signal },
                );
                if (controller.signal.aborted) return;
                setResultScope(requestScope);
                setFetchedProducts(result.content);
                setPageInfo({
                    totalElements: result.page.totalElements,
                    totalPages: result.page.totalPages,
                });
                setFetchStatus(result.content.length === 0 ? 'empty' : 'success');
            } catch (error) {
                // Se ignora el AbortError porque es el comportamiento esperado al seguir escribiendo
                if (error.name === 'AbortError') return;
                if (controller.signal.aborted) return;
                setResultScope(requestScope);
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
        const debounceTimer = setTimeout(fetchProducts, hasTerm ? SEARCH_DEBOUNCE_MS : 0);

        // Funcion de limpieza: Se ejecuta si las dependencias cambian ANTES de los 400ms,
        // cancelando el timer actual y la peticion HTTP en vuelo.
        return () => {
            clearTimeout(debounceTimer);
            controller.abort();
        };
    }, [isIdle, isSoon, isComposing, catalogReady, hasTerm, searchTerm, categoryId, sectionId,
        storeKey, validStoreKey, minPrice, maxPrice, pageNumber, requestScope]);

    // =========================================================================
    // MANEJADORES DE UI
    // =========================================================================
    function showAllProducts() {
        setSelectedCategoryId(null);
        setSelectedSectorId(null);
        setShowAll(true);
        changeFilters((next) => next.delete('sector'));
    }

    // Ojo: las pestañas cambian el estado local pero NO escriben ?sector= en la URL. Por eso un
    // enlace copiado despues de cambiar de pestaña apunta al sector con el que se entro a la pagina.
    function toggleSector(id) {
        setShowAll(false);
        const nextSectorId = selectedSectorId === id ? null : id;
        setSelectedSectorId(nextSectorId);
        setSelectedCategoryId(null);
        changeFilters((next) => {
            setOrDelete(next, 'sector', nextSectorId);
            next.delete(PARAM_STORE);
        });
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
        (categoryId !== null ? 1 : 0)
        + (validStoreKey ? validStoreKey.split(STORE_SEPARATOR).length : 0)
        + (minPrice !== '' || maxPrice !== '' ? 1 : 0);

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

    const noFilters = categoryId === null;

    function clearCategory() {
        setSelectedCategoryId(null);
        setShowAll(true);
        backToFirstPage();
    }

    function clearSearch() {
        setSearchTerm('');
        changeFilters((next) => next.delete('q'));
        searchInputRef.current?.focus();
    }

    return (
        <div className={styles['search-page']}>
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
                        ref={searchInputRef}
                        type="search"
                        className={styles['search-bar__input']}
                        placeholder="Ej. Arroz, Leche, Chocolatina..."
                        value={searchTerm}
                        onChange={handleSearchChange}
                        onCompositionStart={() => setIsComposing(true)}
                        onCompositionEnd={() => setIsComposing(false)}
                        aria-label="Buscar producto por nombre"
                    />
                    {searchTerm && (
                        <button type="button" onClick={clearSearch} aria-label="Limpiar búsqueda"
                            className={styles['search-clear']}>
                            ×
                        </button>
                    )}
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

                            {catalog.status === 'loading' && <p role="status">Cargando filtros…</p>}
                            {catalog.status === 'error' && (
                                <div role="alert">
                                    <p>No pudimos cargar los filtros de esta sección.</p>
                                    <button type="button" onClick={catalog.retry}>Reintentar</button>
                                </div>
                            )}
                            {catalogReady && categories.length === 0 && <p>No hay categorías activas disponibles.</p>}
                            {catalogReady && stores.length === 0 && <p>No hay tiendas activas disponibles.</p>}
                            {categories.length > 0 && (
                                <div className={styles['filter-card']} role="radiogroup" aria-labelledby="categoria-label">
                                    <span id="categoria-label" className={styles['filter-card__title']}>Categoría</span>
                                    <label className={styles['filter-option']}>
                                        <input
                                            type="radio"
                                            name="categoria"
                                            checked={noFilters}
                                            onChange={clearCategory}
                                        />
                                        <span>Todas</span>
                                    </label>
                                    {categories.map((cat) => (
                                        <label key={cat.id} className={styles['filter-option']}>
                                            <input
                                                type="radio"
                                                name="categoria"
                                                checked={categoryId === cat.id}
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

                            {status === 'loading' && <ResultsSkeleton />}

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

                            {status === 'filters-error' && (
                                <div className={styles['search-state']} role="alert">
                                    <p>No pudimos cargar esta sección. Reintenta desde los filtros.</p>
                                </div>
                            )}

                            {status === 'unknown-section' && (
                                <div className={styles['search-state']} role="alert">
                                    <p>Esta sección no está disponible.</p>
                                    <button type="button" onClick={showAllProducts}>Ver todas las secciones</button>
                                </div>
                            )}

                            {status === 'error' && (
                                <div className={styles['search-state']}>
                                    <p className={styles['search-state__title']}>Algo salió mal</p>
                                    <p>No pudimos conectar con el servidor. Verifica tu conexión.</p>
                                </div>
                            )}

                            {status === 'success' && products.map((product, index) => (
                                <ProductCard key={product.id} product={product} index={index} />
                            ))}
                        </div>

                        {status === 'success' && (
                            <SearchPagination page={pageNumber} totalPages={pageInfo.totalPages} onChange={goToPage} />
                        )}
                    </section>
                </div>
            </div>
        </div>
    );
}
