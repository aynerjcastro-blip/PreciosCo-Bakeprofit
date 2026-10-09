import { useState, useEffect } from 'react';
import styles from './Search.module.css';
// import { API_BASE } from '../config'; 

const API_BASE = 'http://localhost:8080/api'; // Ajusta segun el entorno

export default function Search() {
    //Definicion de Estados
    const [searchTerm, setSearchTerm] = useState('');
    const [categories, setCategories] = useState([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState(null);
    const [products, setProducts] = useState([]);
    const [status, setStatus] = useState('idle'); // 'idle', 'loading', 'empty', 'error'

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
    }, []); // El array vacío asegura que esto solo corra al montar el componente

    // Logica de busqueda con Debounce y AbortController
    useEffect(() => {
        // Si no hay termino de busqueda ni categoría, opcionalmente limpiamos o mostramos todo
        const controller = new AbortController();

        const fetchProducts = async () => {
            setStatus('loading');
            const params = new URLSearchParams();
            if (searchTerm.trim()) params.set('name', searchTerm.trim());
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

        // Implementación del Debounce (400ms)
        const debounceTimer = setTimeout(() => {
            fetchProducts();
        }, 400);

        // Cleanup function: cancela el timer y la petición anterior si el usuario sigue escribiendo
        return () => {
            clearTimeout(debounceTimer);
            controller.abort();
        };
    }, [searchTerm, selectedCategoryId]); // Se re-ejecuta cada vez que cambian estos estados

    return (
        <main className={styles['search-page']}>
            <div className={styles['search-page__container']}>
                <h1 className={styles['search-page__title']}>Busca tu producto</h1>
                <p className={styles['search-page__subtitle']}>
                    Escribe un nombre o filtra por categoría
                </p>

                {/* Barra de búsqueda controlada por React */}
                <div className={styles['search-bar']}>
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
                        className={`${styles['category-filter__chip']} ${selectedCategoryId === null ? styles['category-filter__chip--active'] : ''}`}
                        onClick={() => setSelectedCategoryId(null)}
                    >
                        Todas
                    </button>
                    {categories.map((cat) => (
                        <button
                            key={cat.id}
                            className={`${styles['category-filter__chip']} ${selectedCategoryId === cat.id ? styles['category-filter__chip--active'] : ''}`}
                            onClick={() => setSelectedCategoryId(cat.id)}
                        >
                            {cat.name}
                        </button>
                    ))}
                </div>

                {/* Renderizado Condicional del Estado de la Búsqueda */}
                <div className={styles['search-results']} aria-live="polite">
                    {status === 'loading' && (
                        <div className={styles['search-state']}>
                            <p className={styles['search-state__title']}>Cargando</p>
                            <p>Buscando productos...</p>
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
                        <article key={product.id} className={styles['product-card']}>
                            <span className={styles['product-card__category']}>{product.category}</span>
                            <h3 className={styles['product-card__name']}>{product.name}</h3>
                            <p className={styles['product-card__unit']}>{product.unit || ''}</p>
                        </article>
                    ))}
                </div>
            </div>
        </main>
    );
}