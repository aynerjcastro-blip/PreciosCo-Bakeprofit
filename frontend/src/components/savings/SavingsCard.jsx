import { Link } from 'react-router-dom';
import { useSavingsDeal } from '../../hooks/useSavingsDeal';
// Se asume que el archivo CSS central de esta vista fue renombrado a AuthPage.module.css
import styles from '../../pages/AuthPage.module.css';

/**
 * Componente visual único para la tarjeta de ahorro.
 * ¿Por qué? Al usar el custom hook 'useSavingsDeal', este componente se vuelve un "dumb component".
 * Su única responsabilidad es recibir el 'deal' procesado y renderizar el DOM, aplicando separación de intereses.
 */
export default function SavingsCard({ searchTerm = 'Audifonos' }) {
    const { deal, isLoading } = useSavingsDeal(searchTerm);

    if (isLoading || !deal) return null;

    return (
        <Link to={`/comparador?productId=${deal.id}`} className={styles['savings-card']}>
            <div className={styles['savings-card__badge']}>
                Ahorras<br />${deal.savings.toLocaleString('es-CO')}
            </div>
            <div className={styles['savings-card__product']}>
                {deal.image ? (
                    <img src={deal.image} alt={deal.name} className={styles['savings-card__img']} />
                ) : (
                    <div className={styles['savings-card__img-placeholder']} />
                )}
                <h4 className={styles['savings-card__name']}>{deal.name}</h4>
            </div>
            <div className={styles['savings-card__comparison']}>
                <div className={styles['savings-card__price-box']}>
                    <span className={styles['savings-card__price--best']}>
                        ${deal.bestPrice.toLocaleString('es-CO')}
                    </span>
                    <span className={styles['savings-card__store']}>en {deal.bestStore}</span>
                </div>
                <div className={styles['savings-card__line']}>
                    <svg width={40} height={12} viewBox="0 0 40 12" fill="none" aria-hidden="true">
                        <path d="M0 6H40M40 6L35 1M40 6L35 11" stroke="#FF9F1C" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                </div>
                <div className={styles['savings-card__price-box']}>
                    <span className={styles['savings-card__price--worst']}>
                        ${deal.worstPrice.toLocaleString('es-CO')}
                    </span>
                    <span className={styles['savings-card__store']}>en {deal.worstStore}</span>
                </div>
            </div>
        </Link>
    );
}
