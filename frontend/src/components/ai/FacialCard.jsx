import s from '@/components/ai/FacialCard.module.css';
import { CheckIcon, ExternalIcon, MissingIcon } from '@/components/ui/icons/ChatIcons';

const COP = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
});

function formatPrice(value) {
    return value == null ? '' : COP.format(Number(value));
}

const SKIN_LABEL = {
    grasa: 'Piel grasa',
    seca: 'Piel seca',
    mixta: 'Piel mixta',
    normal: 'Piel normal',
    sensible: 'Piel sensible',
};

const NEED_LABEL = {
    acne: 'Acné',
    brillo: 'Brillo',
    poros: 'Poros',
    manchas: 'Manchas',
    arrugas: 'Arrugas',
    hidratacion: 'Hidratación',
    sensibilidad: 'Sensibilidad',
    ojeras: 'Ojeras',
    luminosidad: 'Luminosidad',
    proteccion_solar: 'Protección solar',
};

function PriceLink({ option }) {
    const content = (
        <>
            <span>{formatPrice(option.price)}</span>
            <span className={s.store}>{option.store}{option.url && <> <ExternalIcon /></>}</span>
        </>
    );
    return option.url ? (
        <a className={s.price} href={option.url} target="_blank" rel="noopener noreferrer"
            title={`Ver en ${option.store}`}>
            {content}
        </a>
    ) : (
        <span className={s.price}>{content}</span>
    );
}

/**
 * Productos recomendados por tipo (HU-40, RF32). Las opciones ya vienen
 * filtradas por presupuesto desde el backend: aquí solo se muestran.
 *
 * @param {{ recommendation: { skinType: string, needs: string[], budget: number|null, groups: Array } }} props
 */
export default function FacialCard({ recommendation }) {
    const { skinType, needs = [], budget, groups = [] } = recommendation;
    if (groups.length === 0) return null;

    return (
        <section className={s.card} aria-label="Productos recomendados">
            <header className={s.header}>
                <span className={s.title}>Tu rutina en el catálogo</span>
                <div className={s.tags}>
                    {skinType && <span className={s.tag}>{SKIN_LABEL[skinType] ?? skinType}</span>}
                    {needs.map(n => <span key={n} className={s.tag}>{NEED_LABEL[n] ?? n}</span>)}
                    <span className={`${s.tag} ${s.budgetTag}`}>
                        {budget ? `Hasta ${formatPrice(budget)} c/u` : 'Sin presupuesto'}
                    </span>
                </div>
            </header>

            <ul className={s.list}>
                {groups.map((g, i) => {
                    const best = g.withinBudget?.[0];
                    const alternatives = g.withinBudget?.slice(1) ?? [];
                    return (
                        <li key={g.type} className={`${s.group} ${best ? '' : s.none}`} style={{ '--i': i }}>
                            {best ? <CheckIcon className={s.okIcon} /> : <MissingIcon className={s.missIcon} />}
                            <div className={s.info}>
                                <span className={s.type}>{g.label}</span>
                                {best ? (
                                    <>
                                        <span className={s.name}>{best.name}</span>
                                        {best.brand && <span className={s.brand}>{best.brand}</span>}
                                        {alternatives.length > 0 && (
                                            <span className={s.alt}>
                                                {alternatives.length === 1 ? 'Otra opción: ' : 'Otras opciones: '}
                                                {alternatives.map((a, j) => (
                                                    <span key={a.productId ?? j}>
                                                        {j > 0 && ' · '}
                                                        {a.url
                                                            ? <a href={a.url} target="_blank" rel="noopener noreferrer">{a.name}</a>
                                                            : a.name}
                                                        {' '}({formatPrice(a.price)})
                                                    </span>
                                                ))}
                                            </span>
                                        )}
                                    </>
                                ) : (
                                    <span className={s.name}>
                                        {g.cheapestOverBudget
                                            ? 'Sin opciones dentro de tu presupuesto'
                                            : 'No hay productos de este tipo en el catálogo'}
                                    </span>
                                )}
                                {!best && g.cheapestOverBudget && (
                                    <span className={s.alt}>
                                        La más económica: {g.cheapestOverBudget.name}
                                    </span>
                                )}
                            </div>
                            {best && <PriceLink option={best} />}
                            {!best && g.cheapestOverBudget && (
                                <span className={s.overBudget}>
                                    <PriceLink option={g.cheapestOverBudget} />
                                    <span className={s.overLabel}>Supera el presupuesto</span>
                                </span>
                            )}
                        </li>
                    );
                })}
            </ul>
        </section>
    );
}
