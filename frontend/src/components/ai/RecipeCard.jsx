import s from '@/components/ai/RecipeCard.module.css';
import { CheckIcon, ExternalIcon, MissingIcon } from '@/components/ui/icons/ChatIcons';

const COP = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
});

function formatPrice(value) {
    return value == null ? '' : COP.format(Number(value));
}

/**
 * Disponibilidad de los ingredientes de una receta en el catálogo
 * (SCRUM-325, RF30). Los datos vienen del backend, que los buscó en la base
 * de datos: aquí solo se muestran.
 *
 * @param {{ recipe: { ingredients: Array, estimatedTotal: number } }} props
 */
export default function RecipeCard({ recipe }) {
    const ingredients = recipe.ingredients ?? [];
    const available = ingredients.filter(i => i.available);
    const missing = ingredients.filter(i => !i.available);

    return (
        <section className={s.card} aria-label="Ingredientes en el catálogo">
            <header className={s.header}>
                <span className={s.title}>Ingredientes en el catálogo</span>
                <span className={s.count}>{available.length} de {ingredients.length}</span>
            </header>

            <ul className={s.list}>
                {available.map((item, i) => (
                    <li key={`a-${i}`} className={s.item} style={{ '--i': i }}>
                        <CheckIcon className={s.okIcon} />
                        <div className={s.info}>
                            <span className={s.name}>
                                {item.name}
                                {item.quantity && <span className={s.qty}> · {item.quantity}</span>}
                            </span>
                            <span className={s.product}>{item.productName}</span>
                        </div>
                        {item.url ? (
                            <a
                                className={s.price}
                                href={item.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                title={`Ver en ${item.storeName}`}
                            >
                                <span>{formatPrice(item.price)}</span>
                                <span className={s.store}>{item.storeName} <ExternalIcon /></span>
                            </a>
                        ) : (
                            <span className={s.price}>
                                <span>{formatPrice(item.price)}</span>
                                <span className={s.store}>{item.storeName}</span>
                            </span>
                        )}
                    </li>
                ))}

                {missing.map((item, i) => (
                    <li key={`m-${i}`} className={`${s.item} ${s.missing}`} style={{ '--i': available.length + i }}>
                        <MissingIcon className={s.missIcon} />
                        <div className={s.info}>
                            <span className={s.name}>
                                {item.name}
                                {item.quantity && <span className={s.qty}> · {item.quantity}</span>}
                            </span>
                            <span className={s.product}>No está en el catálogo</span>
                        </div>
                    </li>
                ))}
            </ul>

            {available.length > 0 && (
                <footer className={s.footer}>
                    <span>Total aproximado (mejor precio por producto)</span>
                    <strong>{formatPrice(recipe.estimatedTotal)}</strong>
                </footer>
            )}
        </section>
    );
}
