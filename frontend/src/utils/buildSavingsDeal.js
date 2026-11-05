/**
 * Extrae el cálculo del ahorro a una función pura.
 * ¿Por qué? Separa la lógica de negocio (matemáticas) de React.
 * Esto hace que la función sea predecible, reutilizable y fácil de probar con Jest,
 * sin necesidad de simular componentes visuales ni red.
 */
export function buildSavingsDeal(product, prices) {
    if (!product || !prices || prices.length < 2) return null;

    const best = prices[0];
    const worst = prices[prices.length - 1];
    const savings = worst.value - best.value;

    if (savings <= 0) return null;

    return {
        id: product.id,
        name: product.name,
        image: best.imageUrl || worst.imageUrl,
        bestPrice: best.value,
        bestStore: best.storeName,
        worstPrice: worst.value,
        worstStore: worst.storeName,
        savings
    };
}
