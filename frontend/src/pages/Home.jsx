import { Link } from 'react-router-dom';
import styles from './Home.module.css';

export default function Home() {
    // Categorias para los filtros rapidos
    const filterCategories = [
        "Comida",
        "Electrodomésticos",
        "Ropa",
        "Otros Más"
    ];

    // Datos estructurados de prueba para los testimonios
    const testimonialsData = [
        {
            id: 1,
            company: "Nombre de la empresa",
            quote: "Opinión de la empresa",
            avatarUrl: "/assets/images/default-avatar.png",
            authorName: "Nombre de la persona",
            role: "Cargo de la persona"
        },
        {
            id: 2,
            company: "Nombre de la empresa",
            quote: "Opinión de la empresa",
            avatarUrl: "/assets/images/default-avatar.png",
            authorName: "Nombre de la persona",
            role: "Cargo de la persona"
        },
        {
            id: 3,
            company: "Nombre de la empresa",
            quote: "Opinión de la empresa",
            avatarUrl: "/assets/images/default-avatar.png",
            authorName: "Nombre de la persona",
            role: "Cargo de la persona"
        }
    ];

    return (
        <section>
            {/* Seccion Hero / Llamada a la accion */}
            <div className={styles.hero}>
                <img
                    className={styles.hero__image}
                    src="/assets/images/isometric-flat-3d-illustration-online-shopping-concept-with-ecommerce-app_18660-4570-removebg-preview.png"
                    alt="Ilustración de compra en línea"
                />
                <h2 className={styles.hero__title}>Encuentra Tu Producto al Mejor Precio</h2>
                <p className={styles.hero__text}>Rápido y Sencillo</p>
                <div className={styles.hero__actions}>
                    <Link to="/buscar" className={styles.hero__button}>
                        Compara YA
                    </Link>
                </div>
            </div>

            {/* Seccion de Filtros Rapidos */}
            <div className={styles['quick-filters']}>
                <h2 className={styles['quick-filters__title']}>Los Mejores Precios Del Mercado</h2>
                {filterCategories.map((category, index) => (
                    <button key={index} className={styles['quick-filters__chip']}>
                        {category}
                    </button>
                ))}
                <p className={styles['quick-filters__note']}>Imágenes de referencia</p>
            </div>

            {/* Seccion Sobre Nosotros */}
            <div id="sobre-nosotros" className={styles.about}>
                <h2 className={styles.about__title}>Quienes Somos</h2>
                <img
                    className={styles.about__image}
                    src="/assets/images/LogoPreciosCo.png"
                    alt="Logo PreciosCo"
                    width="400"
                />
                <p className={styles.about__eslogan}>
                    Somos PreciosCo, nacimos con una misión clara: ayudarte a tomar
                    mejores decisiones de compra, cuidando tu bolsillo y ahorrándote
                    tiempo.
                </p>
                <p className={styles.about__eslogan}>
                    Evolucionamos desde una herramienta de consulta ágil en Telegram hacia
                    una plataforma web completa diseñada para centralizar, comparar y
                    transparentar la información de precios de los principales
                    establecimientos del país.
                </p>
            </div>

            {/* Seccion Como Funciona */}
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
                                Ingresa el nombre, marca o categoría del producto. Compara
                                precios al instante entre D1, Jumbo, Éxito y Olímpica ordenados
                                de menor a mayor.
                            </p>
                        </li>
                        <li className={styles['steps-list__item']}>
                            <h3 className={styles['steps-list__item-title']}>Activa Alertas de Ahorro</h3>
                            <p className={styles['steps-list__item-text']}>
                                Sigue tus productos favoritos y configura un precio objetivo. Te
                                avisamos por correo o en la plataforma apenas bajen de precio.
                            </p>
                        </li>
                        <li className={styles['steps-list__item']}>
                            <h3 className={styles['steps-list__item-title']}>Consulta a la IA</h3>
                            <p className={styles['steps-list__item-text']}>
                                Usa nuestros asistentes de inteligencia artificial para recibir
                                recomendaciones de cuidado facial, opciones de droguería o
                                sugerencias de recetas.
                            </p>
                        </li>
                    </ol>
                </div>
            </div>

            {/* Seccion de Testimonios */}
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