const params = new URLSearchParams(window.location.search);
const productId = params.get('productId');

async function loadComparison() {
    if (!productId) {
        document.getElementById('error-msg').style.display = 'block';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/prices/compare?productId=${productId}`);

        if (!res.ok) throw new Error('Error al consultar precios');

        const prices = await res.json();

        if (prices.length === 0) {
            document.getElementById('error-msg').style.display = 'block';
            return;
        }

        
        document.getElementById('product-name').textContent = prices[0].productName;

        const container = document.getElementById('results-container');
        container.innerHTML = '';

        prices.forEach((item, index) => {
            const card = document.createElement('div');
            card.className = `price-card ${index === 0 ? 'price-card--best' : ''}`;

            card.innerHTML = `
                ${index === 0 ? '<span class="price-card__badge">Mejor precio</span>' : ''}
                <h3 class="price-card__store">${item.storeName}</h3>
                <p class="price-card__value">$${Number(item.value).toLocaleString('es-CO')}</p>
                <p class="price-card__date">Actualizado: ${new Date(item.registrationDate).toLocaleDateString('es-CO')}</p>
            `;

            container.appendChild(card);
        });

    } catch (err) {
        console.error(err);
        document.getElementById('error-msg').style.display = 'block';
    }
}

loadComparison();