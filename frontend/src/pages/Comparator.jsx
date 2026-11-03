import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import styles from './comparator.module.css';
import ProductRating from '../components/ProductRating';
import ReviewsSection from '../components/ReviewsSection';
import StoreReviewBar from '../components/StoreReviewBar';
import FollowButton from '../components/FollowButton';

const API_BASE = 'http://localhost:8080';

// Icono de enlace (cadena)
function LinkIcon() {
    return (
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M10 13a5 5 0 0 0 7.07 0l3-3a5 5 0 0 0-7.07-7.07l-1.5 1.5" />
            <path d="M14 11a5 5 0 0 0-7.07 0l-3 3a5 5 0 0 0 7.07 7.07l1.5-1.5" />
        </svg>
    );
}

// Tarjeta de precio: imagen | datos | botón de enlace
function PriceCard({ item, isBest, productId, onShowReviews, onReviewsChanged }) {    // Si la imagen no carga (o no existe) se muestra el espacio con un marcador
    const [imageFailed, setImageFailed] = useState(false);
    const showImage = item.imageUrl && !imageFailed;

    return (
        <div className={`${styles['price-card']} ${isBest ? styles['price-card--best'] : ''}`}>
            {isBest && <span className={styles['price-card__badge']}>Mejor precio</span>}

            <div className={styles['price-card__media']}>
                {showImage ? (
                    <img
                        className={styles['price-card__image']}
                        src={item.imageUrl}
                        alt={item.source || item.productName}
                        loading="lazy"
                        referrerPolicy="no-referrer"
                        onError={() => setImageFailed(true)}
                    />
                ) : (
                    <span className={styles['price-card__placeholder']} aria-hidden="true">🛒</span>
                )}
            </div>

            <div className={styles['price-card__body']}>
                <h3 className={styles['price-card__name']}>{item.storeName}</h3>
                {item.source && (
                    <p className={styles['price-card__source']}>{item.source}</p>
                )}
                <p className={styles['price-card__value']}>
                    ${Number(item.value).toLocaleString('es-CO')}
                </p>
                <p className={styles['price-card__date']}>
                    Actualizado: {new Date(item.registrationDate).toLocaleDateString('es-CO')}
                </p>
                <StoreReviewBar
                    productId={productId}
                    productName={item.productName}
                    storeId={item.storeId}
                    storeName={item.storeName}
                    onShowReviews={onShowReviews}
                    onChanged={onReviewsChanged}
                />
            </div>

            {item.productUrl ? (
                <a
                    className={styles['price-card__link']}
                    href={item.productUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={`Ver el producto en ${item.storeName}`}
                    title={`Ver en ${item.storeName}`}
                >
                    <LinkIcon />
                </a>
            ) : (
                <span
                    className={`${styles['price-card__link']} ${styles['price-card__link--disabled']}`}
                    title="Enlace no disponible"
                    aria-hidden="true"
                >
                    <LinkIcon />
                </span>
            )}
        </div>
    );
}

export default function Comparator() {
    const [searchParams] = useSearchParams();
    const productId = searchParams.get('productId');
    const navigate = useNavigate();

    const [prices, setPrices] = useState([]);
    const [status, setStatus] = useState(productId ? 'loading' : 'error');
    const [productName, setProductName] = useState('Comparando precios...');

    // Opiniones: tienda elegida en la lista compartida (null = todas) y contador para refrescar
    const [selectedStoreId, setSelectedStoreId] = useState(null);
    const [reviewsVersion, setReviewsVersion] = useState(0);

    useEffect(() => {
        if (!productId) return;

        const fetchPrices = async () => {
            try {
                const res = await fetch(`${API_BASE}/api/prices/compare?productId=${productId}`);
                if (!res.ok) throw new Error('Error al consultar precios');
                const data = await res.json();
                if (data.length === 0) { setStatus('empty'); return; }
                setProductName(data[0].productName);
                setPrices(data);
                setStatus('success');
            } catch (err) {
                console.error(err);
                setStatus('error');
            }
        };

        fetchPrices();
    }, [productId]);

    // Al pulsar el promedio de una tarjeta: filtra la lista por esa tienda y baja hasta ella
    const handleShowReviews = (storeId) => {
        setSelectedStoreId(storeId);
        document.getElementById('opiniones')?.scrollIntoView({ behavior: 'smooth' });
    };

    return (
        <main className={styles.comparador}>
            <button className={styles.comparador__back} onClick={() => navigate(-1)}>
                ← Volver
            </button>

            <div className={styles.comparador__header}>
                <h1 className={styles.comparador__title}>{productName}</h1>
                {status === 'success' && <ProductRating productId={productId} refreshKey={reviewsVersion} />}
                {status === 'success' && <ProductRating productId={productId} refreshKey={reviewsVersion} />}
                {status === 'success' && <FollowButton productId={productId} productName={productName} />}
                <p className={styles.comparador__subtitle}>
                    Precios ordenados de menor a mayor entre tiendas activas
                </p>
            </div>

            {status === 'loading' && <p className={styles.comparador__error}>Cargando precios...</p>}
            {(status === 'error' || status === 'empty') && (
                <p className={styles.comparador__error}>No se encontraron precios para este producto.</p>
            )}

            {status === 'success' && (
                <>
                    <div className={styles['price-list']}>
                        {prices.map((item, index) => (
                            <PriceCard
                                key={item.storeId}
                                item={item}
                                isBest={index === 0}
                                productId={productId}
                                onShowReviews={handleShowReviews}
                                onReviewsChanged={() => setReviewsVersion((version) => version + 1)}
                            />
                        ))}
                    </div>

                    <ReviewsSection
                        productId={productId}
                        stores={prices.map((item) => ({ id: item.storeId, name: item.storeName }))}
                        selectedStoreId={selectedStoreId}
                        onSelectStore={setSelectedStoreId}
                        refreshKey={reviewsVersion}
                    />
                </>
            )}
        </main>
    );
}