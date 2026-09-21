let currentUser = null;

window.addEventListener('DOMContentLoaded', async () => {
    await loadSession();
    await loadProducts();
    checkAddedMessage();
});

async function loadSession() {
    try {
        const res = await fetch('session-info');
        const data = await res.json();
        if (!data.loggedIn) {
            window.location.href = 'login.html';
            return;
        }
        currentUser = data;
        if (data.role === 'SELLER') {
            document.getElementById('addProductBtn').style.display = 'inline-block';
        }
    } catch (e) {
        console.error('Session check failed', e);
        window.location.href = 'login.html';
    }
}

async function loadProducts() {
    const grid = document.getElementById('productGrid');
    try {
        const res = await fetch('products');
        const products = await res.json();

        if (products.length === 0) {
            grid.innerHTML = '<p>No products available yet.</p>';
            return;
        }

        grid.innerHTML = products.map(p => `
            <div class="product-card">
                <img src="${p.imageUrl || 'https://via.placeholder.com/300x300?text=Appliance'}" alt="${escapeHtml(p.name)}">
                <div class="info">
                    <div class="name">${escapeHtml(p.name)}</div>
                    <div class="category">${escapeHtml(p.category || '')}</div>
                    <div class="price">₹${Number(p.price).toLocaleString('en-IN')}</div>
                    <div class="stock">${p.stockQuantity > 0 ? p.stockQuantity + ' in stock' : 'Out of stock'}</div>
                    <button onclick="addToCart(${p.productId})" ${p.stockQuantity === 0 ? 'disabled' : ''}>
                        Add to Cart
                    </button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        grid.innerHTML = '<p>Failed to load products. Please refresh.</p>';
        console.error(e);
    }
}

async function addToCart(productId) {
    try {
        const body = new URLSearchParams();
        body.append('productId', productId);
        body.append('quantity', 1);

        const res = await fetch('cart?action=add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body.toString()
        });

        if (res.ok) {
            showMessage('Added to cart!');
        } else {
            showMessage('Could not add to cart. Please log in again.');
        }
    } catch (e) {
        console.error(e);
        showMessage('Something went wrong.');
    }
}

function showMessage(text) {
    const box = document.getElementById('messageBox');
    box.textContent = text;
    box.style.display = 'block';
    setTimeout(() => { box.style.display = 'none'; }, 2500);
}

function checkAddedMessage() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('added') === 'true') {
        showMessage('Product added successfully!');
    }
}

function openModal() {
    document.getElementById('modalOverlay').classList.add('open');
}

function closeModal() {
    document.getElementById('modalOverlay').classList.remove('open');
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
