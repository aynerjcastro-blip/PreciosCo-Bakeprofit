import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import styles from '@/pages/Comparator.module.css';
import ProductRating from '@/components/reviews/ProductRating';
import ReviewsSection from '@/components/reviews/ReviewsSection/ReviewsSection';
import StoreReviewBar from '@/components/reviews/StoreReviewBar/StoreReviewBar';
import FollowButton from '@/components/follows/FollowButton/FollowButton';
import { compareByProduct } from '@/api/prices';

const formatCop = (value) => `$${Number(value).toLocaleString('es-CO')}`;

const formatDate = (value) =>
    new Date(value).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

// Icono de enlace externo
function ExternalIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M14 3h7v7" />
            <path d="M10 14 21 3" />
            <path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5" />
        </svg>
    );
}

// Imagen del producto; si no existe o no carga se muestra un marcador
function Thumb({ src, alt, className, placeholderClassName }) {
    const [failed, setFailed] = useState(false);

    if (!src || failed) {
        return <span className={placeholderClassName} aria-hidden="true">🛒</span>;
    }
    return (
        <img
            className={className}
            src={src}
            alt={alt}
            loading="lazy"
            referrerPolicy="no-referrer"
            onError={() => setFailed(true)}
        />
    );
}

// Resumen superior: cuánto cuesta lo mejor y cuánto se ahorra frente a la tienda más cara
function Summary({ prices }) {
    const cheapest = prices[0];
    const dearest = prices[prices.length - 1];
    const saving = Number(dearest.value) - Number(cheapest.value);
    const savingPct = Math.round((saving / Number(dearest.value)) * 100);
    const bestStores = prices.filter((item) => Number(item.value) === Number(cheapest.value));

    return (
        <dl className={styles.summary}>
            <div className={`${styles.summary__item} ${styles['summary__item--main']}`}>
                <dt>Mejor precio</dt>
                <dd className={styles.summary__value}>{formatCop(cheapest.value)}</dd>
                <dd className={styles.summary__hint}>
                    en {bestStores.map((item) => item.storeName).join(' y ')}
                </dd>
            </div>
            <div className={styles.summary__item}>
                <dt>Puedes ahorrar</dt>
                <dd className={styles.summary__value}>{saving > 0 ? formatCop(saving) : '—'}</dd>
                <dd className={styles.summary__hint}>
                    {saving > 0 ? `${savingPct} % menos que en ${dearest.storeName}` : 'Mismo precio en todas'}
                </dd>
            </div>
            <div className={styles.summary__item}>
                <dt>Tiendas</dt>
                <dd className={styles.summary__value}>{prices.length}</dd>
                <dd className={styles.summary__hint}>con precio vigente</dd>
            </div>
        </dl>
    );
}

// Fila de precio: posición | imagen y tienda | precio y diferencia | acción
function PriceCard({ item, rank, isBest, bestValue, productId, onShowReviews, onReviewsChanged }) {
    const extra = Number(item.value) - bestValue;

    return (
        <li className={`${styles['price-card']} ${isBest ? styles['price-card--best'] : ''}`}>
            <div className={styles['price-card__main']}>
                <span className={styles['price-card__rank']} aria-hidden="true">{rank}</span>

                <div className={styles['price-card__media']}>
                    <Thumb
                        src={item.imageUrl}
                        alt={item.source || item.productName}
                        className={styles['price-card__image']}
                        placeholderClassName={styles['price-card__placeholder']}
                    />
                </div>

                <div className={styles['price-card__info']}>
                    <div className={styles['price-card__heading']}>
                        <h3 className={styles['price-card__store']}>{item.storeName}</h3>
                        {isBest && <span className={styles['price-card__badge']}>Mejor precio</span>}
                    </div>
                    {item.source && <p className={styles['price-card__source']}>{item.source}</p>}
                    {/* RF10, DP-32: la última vez que la tienda confirmó este precio, no cuándo empezó */}
                    <p className={styles['price-card__date']}>
                        Precio confirmado el {formatDate(item.lastSeenAt)}
                    </p>
                </div>

                <div className={styles['price-card__price']}>
                    <p className={styles['price-card__value']}>{formatCop(item.value)}</p>
                    {!isBest && extra > 0 && (
                        <p className={styles['price-card__diff']}>+{formatCop(extra)} más que el mejor</p>
                    )}
                </div>

                {/* RF57: un precio sin página registrada se muestra sin enlace (ni un botón deshabilitado) */}
                {item.productUrl && (
                    <a
                        className={`${styles['price-card__link']} ${isBest ? styles['price-card__link--best'] : ''}`}
                        href={item.productUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`Ver el producto en ${item.storeName} (se abre en una pestaña nueva)`}
                    >
                        Ir a la tienda <ExternalIcon />
                    </a>
                )}
            </div>

            <StoreReviewBar
                productId={productId}
                productName={item.productName}
                storeId={item.storeId}
                storeName={item.storeName}
                onShowReviews={onShowReviews}
                onChanged={onReviewsChanged}
            />
        </li>
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
                const data = await compareByProduct(productId);
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

    // La imagen de la cabecera es la primera que alguna tienda tenga registrada
    const heroImage = prices.find((item) => item.imageUrl)?.imageUrl;
    const bestValue = prices.length ? Number(prices[0].value) : 0;

    return (
        <main className={styles.comparador}>
            <button className={styles.comparador__back} onClick={() => navigate(-1)}>
                ← Volver a los resultados
            </button>

            {status === 'loading' && (
                <p className={styles.comparador__state} role="status">Cargando precios...</p>
            )}
            {(status === 'error' || status === 'empty') && (
                <p className={styles.comparador__state}>No se encontraron precios para este producto.</p>
            )}

            {status === 'success' && (
                <>
                    <header className={styles.product}>
                        <div className={styles.product__media}>
                            <Thumb
                                src={heroImage}
                                alt={productName}
                                className={styles.product__image}
                                placeholderClassName={styles.product__placeholder}
                            />
                        </div>
                        <div className={styles.product__info}>
                            <h1 className={styles.product__title}>{productName}</h1>
                            <ProductRating productId={productId} refreshKey={reviewsVersion} />
                            <FollowButton productId={productId} productName={productName} />
                        </div>
                    </header>

                    <Summary prices={prices} />

                    <section aria-labelledby="titulo-precios">
                        <div className={styles.list__head}>
                            <h2 id="titulo-precios" className={styles.list__title}>Precio en cada tienda</h2>
                            <p className={styles.list__hint}>De menor a mayor · solo tiendas activas</p>
                        </div>

                        <ol className={styles['price-list']}>
                            {prices.map((item, index) => (
                                <PriceCard
                                    key={item.storeId}
                                    item={item}
                                    rank={index + 1}
                                    isBest={Number(item.value) === bestValue}
                                    bestValue={bestValue}
                                    productId={productId}
                                    onShowReviews={handleShowReviews}
                                    onReviewsChanged={() => setReviewsVersion((version) => version + 1)}
                                />
                            ))}
                        </ol>

                        <p className={styles.disclaimer}>
                            Los precios son informativos: la tienda puede cambiarlos en cualquier momento.
                            Verifica el valor final en su página antes de comprar.
                        </p>
                    </section>

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