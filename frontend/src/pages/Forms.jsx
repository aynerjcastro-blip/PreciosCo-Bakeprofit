import Hero from '../components/home/Hero';
import QuickFilters from '../components/home/QuickFilters';

//Importa los estilos de tu módulo local
import styles from './Home.module.css';

export default function Home() {
    return (
        //Aplicas la clase usando la sintaxis de corchetes
        <section className={styles.homeSection}>
            <Hero />
            <QuickFilters />
        </section>
    );
}