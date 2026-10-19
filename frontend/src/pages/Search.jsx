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

import { Link } from 'react-router-dom';
// import { API_BASE } from '../config';

const API_BASE = 'http://localhost:8080/api'; // Ajusta segun el entorno

// Sectores que se muestran como tarjetas.
// `available: false` -> la tarjeta se muestra, pero aun no hay datos en el backend.
// Cuando existan productos de ese sector, basta con cambiarlo a true.
const SECTORS = [
    { id: 'supermercados', label: 'Supermercados', Icon: CartIcon, available: true },
    { id: 'droguerias', label: 'Droguerías', Icon: PillIcon, available: false },
    { id: 'cuidado-facial', label: 'Cuidado facial', Icon: FaceIcon, available: false },
    { id: 'repuestos', label: 'Repuestos', Icon: GearIcon, available: false },
];

// Estado guardado: al volver del comparador se restaura lo que tenias en pantalla
const SAVED_KEY = 'searchState';

function loadSaved() {
    try {
        return JSON.parse(sessionStorage.getItem(SAVED_KEY)) || {};
    } catch {
        return {};
    }
}

export default function Search() {
    const saved = loadSaved();

    //Definicion de Estados
    const [searchTerm, setSearchTerm] = useState(saved.searchTerm ?? '');
    const [categories, setCategories] = useState([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState(saved.selectedCategoryId ?? null);
    const [selectedSectorId, setSelectedSectorId] = useState(saved.selectedSectorId ?? null);
    const [showAll, setShowAll] = useState(saved.showAll ?? false);
    const [products, setProducts] = useState([]);
    const [status, setStatus] = useState('idle'); // 'idle', 'loading', 'empty', 'error', 'soon', 'success'

    const selectedSector = SECTORS.find((s) => s.id === selectedSectorId) || null;

    // Guarda los filtros cada vez que cambian
    useEffect(() => {
        sessionStorage.setItem(
            SAVED_KEY,
            JSON.stringify({ searchTerm, selectedCategoryId, selectedSectorId, showAll })
        );
    }, [searchTerm, selectedCategoryId, selectedSectorId, showAll]);

    // Carga inicial de categorías
    useEffect(() => {
        async function fetchCategories() {
            try {
                const response = await fetch(`${API_BASE}/categories/active`);
                if (!response.ok) throw new Error('Error al cargar categorías');
                const data = await response.json();
                setCategories(data);
            } catch (error) {
                console.error(error);
                //Manejo silencioso: permitimos buscar por texto aunque fallen las categorías
            }
        }
        fetchCategories();
    }, []);

    // Logica de busqueda con Debounce y AbortController
    useEffect(() => {
        const hasTerm = searchTerm.trim() !== '';
        const hasFilter = selectedCategoryId !== null || selectedSectorId !== null || showAll;

        // Sin termino ni filtros: solo se ven las tarjetas de sector
        if (!hasTerm && !hasFilter) {
            setProducts([]);
            setStatus('idle');
            return;
        }

        // Sector sin datos todavia
        if (selectedSector && !selectedSector.available) {
            setProducts([]);
            setStatus('soon');
            return;
        }

        const controller = new AbortController();

        const fetchProducts = async () => {
            setStatus('loading');
            const params = new URLSearchParams();
            if (hasTerm) params.set('name', searchTerm.trim());
            if (selectedCategoryId !== null) params.set('idCategory', selectedCategoryId);

            try {
                const response = await fetch(`${API_BASE}/products/search?${params.toString()}`, {
                    signal: controller.signal,
                });
                if (!response.ok) throw new Error('Error en la petición');

                const data = await response.json();
                setProducts(data);
                setStatus(data.length === 0 ? 'empty' : 'success');
            } catch (error) {
                if (error.name !== 'AbortError') {
                    console.error(error);
                    setStatus('error');
                }
            }
        };

        // Debounce (400ms)
        const debounceTimer = setTimeout(fetchProducts, 400);

        return () => {
            clearTimeout(debounceTimer);
            controller.abort();
        };
    }, [searchTerm, selectedCategoryId, selectedSectorId, selectedSector, showAll]);

    // "Todas": limpia filtros y lista todos los productos
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
                    Escribe un nombre o filtra por categoría
                </p>

                {/* Barra de búsqueda controlada por React */}
                <div className={styles['search-bar']}>
                    <span className={styles['search-bar__icon']}>
                        <SearchIcon />
                    </span>
                    <input
                        type="search"
                        className={styles['search-bar__input']}
                        placeholder="Ej. Arroz, Leche, Chocolatina..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        aria-label="Buscar producto por nombre"
                    />
                </div>

                {/* Filtros dinámicos de categoría */}
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

                {/* Tarjetas de sector */}
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

                {/* Renderizado Condicional del Estado de la Búsqueda */}
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
                            <p>Todavía no tenemos precios de este sector. Mientras tanto, prueba con Supermercados.</p>
                        </div>
                    )}

                    {status === 'empty' && (
                        <div className={styles['search-state']}>
                            <p className={styles['search-state__title']}>Sin resultados</p>
                            <p>No encontramos productos con esos filtros. Prueba con otro nombre o categoría.</p>
                        </div>
                    )}

                    {status === 'error' && (
                        <div className={styles['search-state']}>
                            <p className={styles['search-state__title']}>Algo salió mal</p>
                            <p>No pudimos conectar con el servidor. Verifica tu conexión.</p>
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