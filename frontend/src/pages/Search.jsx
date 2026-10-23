import { useState, useEffect } from 'react';
import styles from './Search.module.css';
import {
    CartIcon,
    PillIcon,
    FaceIcon,
    GearIcon,
    SearchIcon,
    EyeIcon,
} from '../components/Sectoricons.jsx';
import { Link, useSearchParams } from 'react-router-dom';

const API_BASE = 'http://localhost:8080/api';

/**
 * Configuracion estatica de los sectores.
 * Se define fuera del componente para evitar recrear el arreglo en cada render.
 * El campo 'available' controla si el sector hace peticiones o muestra la pantalla "Llega pronto".
 */
const SECTORS = [
    { id: 'supermercados', label: 'Supermercados', Icon: CartIcon, available: true },
    { id: 'droguerias', label: 'Droguerias', Icon: PillIcon, available: false },
    { id: 'cuidado-facial', label: 'Cuidado facial', Icon: FaceIcon, available: false },
    { id: 'repuestos', label: 'Repuestos', Icon: GearIcon, available: false },
];

const SAVED_KEY = 'searchState';

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

export default function Search() {
    const saved = loadSaved();
    const [searchParams, setSearchParams] = useSearchParams();

    // 1. FUENTES DE VERDAD EXTERNAS (URL)
    // Extraemos parametros de la URL para habilitar navegacion directa (ej. links desde el Header).
    const urlQuery = searchParams.get('q');
    const urlSector = searchParams.get('sector');

    // 2. ESTADOS LOCALES
    // Inicializacion inteligente: La prioridad es 1. URL, 2. Sesion guardada, 3. Valor por defecto.
    // Por que: Garantiza que un link externo siempre sobreescriba una busqueda vieja guardada.
    const [searchTerm, setSearchTerm] = useState(urlQuery ?? saved.searchTerm ?? '');
    const [selectedSectorId, setSelectedSectorId] = useState(urlSector ?? saved.selectedSectorId ?? null);

    const [categories, setCategories] = useState([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState(saved.selectedCategoryId ?? null);
    const [showAll, setShowAll] = useState(saved.showAll ?? false);

    const [fetchedProducts, setFetchedProducts] = useState([]);
    // 'idle' = inactivo, 'loading' = cargando, 'empty' = vacio, 'error' = fallo de red, 'success' = exito
    const [fetchStatus, setFetchStatus] = useState('idle');

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
     * Maneja el cambio de texto en el input de busqueda.
     * Por que: Actualiza el estado local inmediatamente, pero limpia la URL. 
     * Si no limpiaramos la URL, el historial del navegador se ensuciaria, 
     * y el estado local chocaria con el parametro estatico '?q=' anterior.
     */
    const handleSearchChange = (e) => {
        const newValue = e.target.value;
        setSearchTerm(newValue);

        if (urlQuery) {
            // Limpia la barra de direcciones reemplazandola por la ruta base
            setSearchParams(new URLSearchParams());
        }
    };

    // =========================================================================
    // VARIABLES DERIVADAS
    // =========================================================================
    // Por que: Se calculan en cada render en lugar de usar estados adicionales 
    // para evitar problemas de sincronizacion ("React anti-pattern: derived state").
    const selectedSector = SECTORS.find((s) => s.id === selectedSectorId) || null;
    const hasTerm = searchTerm.trim() !== '';
    const hasFilter = selectedCategoryId !== null || selectedSectorId !== null || showAll;

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
            const params = new URLSearchParams();

            if (hasTerm) params.set('name', searchTerm.trim());
            if (selectedCategoryId !== null) params.set('idCategory', selectedCategoryId);

            try {
                const response = await fetch(`${API_BASE}/products/search?${params.toString()}`, {
                    signal: controller.signal,
                });
                if (!response.ok) throw new Error('Error en la peticion');

                const data = await response.json();
                setFetchedProducts(data);
                setFetchStatus(data.length === 0 ? 'empty' : 'success');
            } catch (error) {
                // Se ignora el AbortError porque es el comportamiento esperado al seguir escribiendo
                if (error.name !== 'AbortError') {
                    console.error(error);
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
    }, [isIdle, isSoon, hasTerm, searchTerm, selectedCategoryId]);

    // =========================================================================
    // MANEJADORES DE UI
    // =========================================================================
    function showAllProducts() {
        setSelectedCategoryId(null);
        setSelectedSectorId(null);
        setShowAll(true);
    }

    function toggleSector(id) {
        setShowAll(false);
        setSelectedSectorId((current) => (current === id ? null : id));
    }

    const noFilters = selectedCategoryId === null && selectedSectorId === null;

    return (
        <main className={styles['search-page']}>
            <div className={styles['search-page__container']}>
                <h1 className={styles['search-page__title']}>Busca tu producto</h1>
                <p className={styles['search-page__subtitle']}>
                    Escribe un nombre o filtra por categoria
                </p>

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

                {/* FILTROS DE CATEGORIA */}
                <div className={styles['category-filter']}>
                    <button
                        className={`${styles['category-filter__chip']} ${noFilters ? styles['category-filter__chip--active'] : ''}`}
                        onClick={showAllProducts}
                    >
                        Todas
                    </button>
                    {categories.map((cat) => (
                        <button
                            key={cat.id}
                            className={`${styles['category-filter__chip']} ${selectedCategoryId === cat.id ? styles['category-filter__chip--active'] : ''}`}
                            onClick={() => {
                                setShowAll(false);
                                setSelectedCategoryId(selectedCategoryId === cat.id ? null : cat.id);
                            }}
                        >
                            {cat.name}
                        </button>
                    ))}
                </div>

                {/* TARJETAS DE SECTOR */}
                <div className={styles['sector-grid']}>
                    {SECTORS.map(({ id, label, Icon }) => {
                        const active = selectedSectorId === id;
                        return (
                            <article
                                key={id}
                                className={`${styles['sector-card']} ${active ? styles['sector-card--active'] : ''}`}
                            >
                                <div className={styles['sector-card__icon']}>
                                    <Icon />
                                </div>
                                <h2 className={styles['sector-card__title']}>{label}</h2>
                                <button
                                    type="button"
                                    className={styles['sector-card__button']}
                                    onClick={() => toggleSector(id)}
                                    aria-pressed={active}
                                    aria-label={`Ver ${label}`}
                                >
                                    <EyeIcon />
                                    <span>Ver</span>
                                </button>
                            </article>
                        );
                    })}
                </div>

                {/* RENDERIZADO DE RESULTADOS */}
                {/* aria-live="polite" notifica a lectores de pantalla los cambios sin interrumpir */}
                <div className={styles['search-results']} aria-live="polite">
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
                            <p>Todavia no tenemos precios de este sector. Mientras tanto, prueba con Supermercados.</p>
                        </div>
                    )}

                    {status === 'empty' && (
                        <div className={styles['search-state']}>
                            <p className={styles['search-state__title']}>Sin resultados</p>
                            <p>No encontramos productos con esos filtros. Prueba con otro nombre o categoria.</p>
                        </div>
                    )}

                    {status === 'error' && (
                        <div className={styles['search-state']}>
                            <p className={styles['search-state__title']}>Algo salio mal</p>
                            <p>No pudimos conectar con el servidor. Verifica tu conexion.</p>
                        </div>
                    )}

                    {status === 'success' && products.map((product) => (
                        <Link
                            key={product.id}
                            to={`/comparador?productId=${product.id}`}
                            className={styles['product-card']}
                        >
                            <span className={styles['product-card__category']}>{product.category}</span>
                            <h3 className={styles['product-card__name']}>{product.name}</h3>
                            <p className={styles['product-card__unit']}>{product.unit || ''}</p>
                        </Link>
                    ))}
                </div>
            </div>
        </main>
    );
}