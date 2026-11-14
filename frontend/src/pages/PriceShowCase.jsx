import { useState } from 'react';
import { Link } from 'react-router-dom';
import Icon from './Icon';
import StoreLogo from './StoreLogo';
import styles from './PriceShowcase.module.css';

const money = (value) => `$${Number(value).toLocaleString('es-CO')}`;

// "11 nov": fecha en que la tienda confirmó el precio por última vez (RF10)
function shortDate(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? ''
        : date.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
}

/**
 * Comparador de ejemplo con productos REALES: cada fila es el precio vigente de una tienda.
 * Las barras son proporcionales al precio más alto, así la diferencia se ve de un vistazo.
 * Nada de este componente está escrito a mano: si no hay productos comparables, no se dibuja.
 *
 * Los botones de arriba usan aria-pressed (no role="tab"): el patrón de pestañas exige además
 * navegar con las flechas del teclado, y un grupo de botones es más simple y accesible.
 */
export default function PriceShowcase({ products }) {
    const [selectedId, setSelectedId] = useState(products[0]?.id);
    const product = products.find((p) => p.id === selectedId) ?? products[0];
    if (!product) return null;

    // Orden de menor a mayor precio (RF09): la primera fila es la mejor opción (RF08)
    const offers = [...product.offers].sort((a, b) => Number(a.value) - Number(b.value));
    const min = Number(offers[0].value);
    const max = Number(offers[offers.length - 1].value);
    const saving = max - min;

    return (
        <div className={styles.card}>
            <div className={styles.tabs} role="group" aria-label="Producto de ejemplo">
                {products.map((p) => (
                    <button
                        key={p.id}
                        type="button"
                        className={`${styles.tab} ${p.id === product.id ? styles.tabActive : ''}`}
                        aria-pressed={p.id === product.id}
                        onClick={() => setSelectedId(p.id)}
                    >
                        {p.name}
                    </button>
                ))}
            </div>

            <div className={styles.head}>
                <div>
                    <h3 className={styles.name}>{product.name}</h3>
                    <p className={styles.meta}>
                        {[product.brand, product.unit].filter(Boolean).join(' · ') || 'Precio vigente por tienda'}
                    </p>
                </div>
                <p className={styles.saving}>
                    <span>Diferencia entre la tienda más barata y la más cara</span>
                    <strong>{money(saving)}</strong>
                </p>
            </div>

            <ul className={styles.rows}>
                {offers.map((offer) => {
                    const value = Number(offer.value);
                    const best = value === min;
                    const date = offer.lastSeenAt ? shortDate(offer.lastSeenAt) : '';
                    return (
                        <li key={offer.storeId} className={`${styles.row} ${best ? styles.rowBest : ''}`}>
                            <StoreLogo name={offer.storeName} />
                            <div className={styles.store}>
                                <p className={styles.storeName}>
                                    {offer.storeName}
                                    {best && <span className={styles.badge}>Más barato</span>}
                                </p>
                                {date && <p className={styles.date}>Precio confirmado el {date}</p>}
                            </div>
                            {/* La barra es solo visual: el precio escrito ya dice lo mismo */}
                            <div className={styles.track} aria-hidden="true">
                                <div className={styles.bar} style={{ width: `${(value / max) * 100}%` }} />
                            </div>
                            <div className={styles.price}>
                                <strong>{money(value)}</strong>
                                {!best && <span>+{money(value - min)}</span>}
                            </div>
                            {offer.productUrl ? (
                                <a
                                    className={styles.external}
                                    href={offer.productUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    aria-label={`Ir a ${offer.storeName} (se abre en una pestaña nueva)`}
                                >
                                    <Icon name="link" size={16} />
                                </a>
                            ) : (
                                <span className={styles.externalSlot} aria-hidden="true" />
                            )}
                        </li>
                    );
                })}
            </ul>

            <div className={styles.foot}>
                <p>Precios informativos: confirma siempre en la tienda antes de comprar.</p>
                <Link to={`/comparador?productId=${product.id}`} className={styles.more}>
                    Ver comparación completa <Icon name="arrow" size={16} />
                </Link>
            </div>
        </div>
    );
}