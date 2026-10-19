import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import apiClient from '../api/client'; // Importamos el cliente API centralizado
import styles from './comparator.module.css';

export default function Comparator() {
    const [searchParams] = useSearchParams();
    const productId = searchParams.get('productId');
    const navigate = useNavigate();

    const [prices, setPrices] = useState([]);
    const [status, setStatus] = useState(productId ? 'loading' : 'error');
    const [productName, setProductName] = useState('Comparando precios...');

    useEffect(() => {
        if (!productId) return;

        const fetchPrices = async () => {
            try {
                // Usamos apiClient.get en lugar de fetch manual y BASE_URL
                const data = await apiClient.get(`/api/prices/compare?productId=${productId}`);
                
                if (!data || data.length === 0) { 
                    setStatus('empty'); 
                    return; 
                }
                
                setProductName(data[0].productName);
                setPrices(data);
                setStatus('success');
            } catch (err) {
                console.error("Error al consultar precios:", err.message);
                setStatus('error');
            }
        };

        fetchPrices();
    }, [productId]);

    return (
        <main className={styles.comparador}>
            <button className={styles.comparador__back} onClick={() => navigate(-1)}>
                ← Volver
            </button>

            <div className={styles.comparador__header}>
                <h1 className={styles.comparador__title}>{productName}</h1>
                <p className={styles.comparador__subtitle}>
                    Precios ordenados de menor a mayor entre tiendas activas
                </p>
            </div>

            {status === 'loading' && <p className={styles.comparador__error}>Cargando precios...</p>}
            {(status === 'error' || status === 'empty') && (
                <p className={styles.comparador__error}>No se encontraron precios para este producto.</p>
            )}

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