import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import styles from './comparator.module.css';

// Asegurate de ajustar la URL base segun tu entorno
const API_BASE = 'http://localhost:8080';

export default function Comparator() {
    // Hook de React Router para leer los parametros de la URL
    const [searchParams] = useSearchParams();
    const productId = searchParams.get('productId');

    // Definicion de estados: validamos el productId desde el inicio de forma segura
    const [prices, setPrices] = useState([]);
    const [status, setStatus] = useState(productId ? 'loading' : 'error');
    const [productName, setProductName] = useState('Comparando precios...');

    // Efecto que se ejecuta al montar el componente o si cambia el productId
    useEffect(() => {
        // Si no hay ID, detenemos el efecto aqui sin hacer llamadas innecesarias a setState
        if (!productId) return;

        const fetchPrices = async () => {
            try {
                const res = await fetch(`${API_BASE}/api/prices/compare?productId=${productId}`);
                if (!res.ok) throw new Error('Error al consultar precios');

                const data = await res.json();

                if (data.length === 0) {
                    setStatus('empty');
                    return;
                }

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
    return (
        <main className={styles.comparador}>
            <div className={styles.comparador__header}>
                <h1 className={styles.comparador__title}>{productName}</h1>
                <p className={styles.comparador__subtitle}>
                    Precios ordenados de menor a mayor entre tiendas activas
                </p>
            </div>

            {/* Manejo de estados de carga y error */}
            {status === 'loading' && (
                <p className={styles.comparador__error}>Cargando precios...</p>
            )}

            {(status === 'error' || status === 'empty') && (
                <p className={styles.comparador__error}>
                    No se encontraron precios para este producto.
                </p>
            )}

            {/* Renderizado de las tarjetas de precios si la peticion es exitosa */}
            {status === 'success' && (
                <div className={styles['price-list']}>
                    {prices.map((item, index) => (
                        <div
                            key={index}
                            className={`${styles['price-card']} ${index === 0 ? styles['price-card--best'] : ''}`}
                        >
                            {index === 0 && (
                                <span className={styles['price-card__badge']}>Mejor precio</span>
                            )}

                            <h3 className={styles['price-card__store']}>{item.storeName}</h3>
                            <p className={styles['price-card__value']}>
                                ${Number(item.value).toLocaleString('es-CO')}
                            </p>
                            <p className={styles['price-card__date']}>
                                Actualizado: {new Date(item.registrationDate).toLocaleDateString('es-CO')}
                            </p>

                            {item.source && (
                                <p className={styles['price-card__source']}>Fuente: {item.source}</p>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </main>
    );
}