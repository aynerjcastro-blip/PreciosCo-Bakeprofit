import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Home.module.css';

export default function Home() {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');

    const handleSearch = (e) => {
        e.preventDefault();
        const term = query.trim();
        if (term) {
            navigate(`/buscar?q=${encodeURIComponent(term)}`);
        } else {
            navigate('/buscar');
        }
    };

    // Convertimos las categorias a objetos para tener el 'id' que lee el buscador
    const filterCategories = [
        { id: 'supermercados', label: 'Supermercados' },
        { id: 'droguerias', label: 'Farmacia' },
        { id: 'repuestos', label: 'Repuestos' },
        { id: 'cuidado-facial', label: 'Belleza' },
        { id: 'tecnologia', label: 'Tecnologia' }
    ];

    // Datos estructurados de prueba para los testimonios (se mantienen igual)
    const testimonialsData = [
        {
            id: 1,
            company: "Tech Solutions",
            quote: "Ahorramos un 20% en insumos tecnológicos el primer mes.",
            avatarUrl: "/assets/images/default-avatar.png",
            authorName: "Carlos Restrepo",
            role: "Gerente de Compras"
        },
        {
            id: 2,
            company: "Familia García",
            quote: "Excelente para comparar los precios del mercado antes de hacer la compra mensual.",
            avatarUrl: "/assets/images/default-avatar.png",
            authorName: "Laura García",
            role: "Ama de Casa"
        },
        {
            id: 3,
            company: "AutoPartes Bogotá",
            quote: "Poder cotizar repuestos rápidamente nos ha dado una ventaja enorme.",
            avatarUrl: "/assets/images/default-avatar.png",
            authorName: "Andrés Medina",
            role: "Mecánico Independiente"
        }
    ];

    return (
        <section>
            {/* NUEVO: Hero Section Centrado con Buscador Gigante */}
            <div className={styles.hero}>
                <h2 className={styles.hero__title}>Encuentra el mejor precio antes de comprar</h2>
                <p className={styles.hero__text}>
                    Compara al instante entre miles de productos de supermercado, farmacia, tecnología y más.
                </p>

                <form className={styles.hero__search} onSubmit={handleSearch} role="search">
                    <span className={`icon icon--search ${styles['hero__search-icon']}`}></span>
                    <input
                        type="search"
                        placeholder="Ej. Arroz Premium, Ibuprofeno, Aceite..."
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        className={styles.hero__input}
                        aria-label="Buscar productos"
                    />
                    <button type="submit" className={styles.hero__submit}>Buscar</button>
                </form>
            </div>

            {/* Seccion de Filtros Rapidos */}
            <div className={styles['quick-filters']}>
                <h2 className={styles['quick-filters__title']}>Explora nuestras categorias</h2>
                {filterCategories.map((category, index) => (
                    <button
                        key={index}
                        className={styles['quick-filters__chip']}
                        onClick={() => {
                            // Si es tecnologia (que aun no tiene ID asignado), mandamos a /buscar general
                            const path = category.id === 'tecnologia'
                                ? '/buscar'
                                : `/buscar?sector=${category.id}`;
                            navigate(path);
                        }}
                    >
                        {category.label}
                    </button>
                ))}
                <p className={styles['quick-filters__note']}>Amplia cobertura para comparar en Colombia</p>
            </div>

            {/* Sección Sobre Nosotros */}
            <div id="sobre-nosotros" className={styles.about}>
                <h2 className={styles.about__title}>Quiénes Somos</h2>
                <img
                    className={styles.about__image}
                    src="/assets/images/LogoPreciosCo.png"
                    alt="Logo PreciosCo"
                    width="400"
                />
                <p className={styles.about__eslogan}>
                    Somos PreciosCo, nacimos con una misión clara: ayudarte a tomar mejores decisiones de compra, cuidando tu bolsillo y ahorrándote tiempo.
                </p>
                <p className={styles.about__eslogan}>
                    Evolucionamos desde una herramienta de consulta ágil en Telegram hacia una plataforma web completa diseñada para centralizar, comparar y transparentar la información de precios de los principales establecimientos del país.
                </p>
            </div>

            {/* Sección Cómo Funciona */}
            <div id="como-funciona" className={styles['how-it-works']}>
                <div className={styles['section-heading']}>
                    <h2 className={styles['section-heading__title']}>¿Cómo funciona PreciosCo?</h2>
                    <p className={styles['section-heading__subtitle']}>
                        Encontrar la mejor opción de compra nunca fue tan fácil.
                    </p>
                </div>
                <div>
                    <ol className={styles['steps-list']}>
                        <li className={styles['steps-list__item']}>
                            <h3 className={styles['steps-list__item-title']}>Busca Y Compara</h3>
                            <p className={styles['steps-list__item-text']}>
                                Ingresa el nombre, marca o categoría del producto. Compara precios al instante ordenados de menor a mayor.
                            </p>
                        </li>
                        <li className={styles['steps-list__item']}>
                            <h3 className={styles['steps-list__item-title']}>Activa Alertas de Ahorro</h3>
                            <p className={styles['steps-list__item-text']}>
                                Sigue tus productos favoritos y configura un precio objetivo. Te avisamos apenas bajen de precio.
                            </p>
                        </li>
                        <li className={styles['steps-list__item']}>
                            <h3 className={styles['steps-list__item-title']}>Consulta a la IA</h3>
                            <p className={styles['steps-list__item-text']}>
                                Usa nuestros asistentes de inteligencia artificial para recibir recomendaciones y alternativas económicas.
                            </p>
                        </li>
                    </ol>
                </div>
            </div>

            {/* Sección de Testimonios */}
            <div id="testimonios" className={styles.testimonials}>
                <h2>Testimonios</h2>
                <ul className={styles['testimonials__grid']}>
                    {testimonialsData.map((testimonial) => (
                        <li key={testimonial.id} className={styles['testimonial-card']}>
                            <h4 className={styles['testimonial-card__label']}>{testimonial.company}</h4>
                            <p className={styles['testimonial-card__quote']}>"{testimonial.quote}"</p>
                            <img
                                className={styles['testimonial-card__avatar']}
                                src={testimonial.avatarUrl}
                                alt={`Foto de ${testimonial.authorName}`}
                                width="40"
                            />
                            <p className={styles['testimonial-card__author']}>{testimonial.authorName}</p>
                            <p className={styles['testimonial-card__role']}>{testimonial.role}</p>
                        </li>
                    ))}
                </ul>
                <button className={styles['testimonials__button']}>Encuentra más Opiniones</button>
            </div>
        </section>
    );
}