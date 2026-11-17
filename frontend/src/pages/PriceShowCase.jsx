import { useState } from 'react';
import { Link } from 'react-router-dom';
import Icon from './Icon';
import ProductImage from './ProductImage';
import StoreLogo from './StoreLogo';
import styles from './PriceShowCase.module.css';

const money = (value) => `$${Number(value).toLocaleString('es-CO')}`;

// "11 nov": fecha en que la tienda confirmó el precio por última vez (RF10)
function shortDate(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? ''
        : date.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
}

/**
 * Comparador de ejemplo con productos REALES. El titular ("$3.150 de diferencia.") se calcula
 * con los precios del producto elegido: cambia solo al cambiar de producto. Nada está escrito a
 * mano, y si no hay productos comparables el componente no se dibuja.
 *
 * Los botones de producto usan aria-pressed (no role="tab"): el patrón de pestañas exige además
 * moverse con las flechas del teclado, y un grupo de botones es más simple y accesible.
 */
export default function PriceShowcase({ products }) {
    const [selectedId, setSelectedId] = useState(products[0]?.id);
    const product = products.find((p) => p.id === selectedId) ?? products[0];
    if (!product) return null;

    // De menor a mayor precio (RF09): la primera fila es la mejor opción (RF08)
    const offers = [...product.offers].sort((a, b) => Number(a.value) - Number(b.value));
    const min = Number(offers[0].value);
    const max = Number(offers[offers.length - 1].value);

    return (
        <div className={styles.layout}>
            <div className={styles.intro}>
                <h2 id="diff-title" className={styles.heading}>{money(max - min)} de diferencia.</h2>
                <p className={styles.text}>
                    Un ejemplo real: es lo que cambia en <strong>{product.name}</strong> entre {offers.length} tiendas.
                    Elige otro producto y mira cuánto se mueve.
                </p>

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

                <p className={styles.note}>
                    Cada tienda confirma su precio en la fecha indicada. Los precios cambian con frecuencia: verifica en la tienda.
                </p>
            </div>

            {/* aria-live: al cambiar de producto, el lector de pantalla anuncia el nuevo contenido */}
            <div className={styles.card} aria-live="polite">
                <div className={styles.photo}>
                    <ProductImage src={product.imageUrl} alt={product.name} />
                </div>

                <div className={styles.panel}>
                    <h3 className={styles.name}>{product.name}</h3>
                    <p className={styles.meta}>{[product.brand, product.unit].filter(Boolean).join(' · ')}</p>

                    <ul className={styles.rows}>
                        {offers.map((offer) => {
                            const value = Number(offer.value);
                            const best = value === min;
                            const date = offer.lastSeenAt ? shortDate(offer.lastSeenAt) : '';
                            return (
                                <li key={offer.storeId} className={`${styles.row} ${best ? styles.rowBest : ''}`}>
                                    <StoreLogo name={offer.storeName} />
                                    <div className={styles.store}>
                                        <p className={styles.storeName}>{offer.storeName}</p>
                                        <p className={styles.date}>
                                            {best ? 'Mejor precio' : `+${money(value - min)}`}
                                            {date && ` · confirmado el ${date}`}
                                        </p>
                                    </div>
                                    <strong className={styles.price}>{money(value)}</strong>
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

                    <Link to={`/comparador?productId=${product.id}`} className={styles.button}>
                        Ver comparación completa <Icon name="arrow" size={16} />
                    </Link>
                </div>
            </div>
        </div>
    );
}