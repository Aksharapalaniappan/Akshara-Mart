window.addEventListener('DOMContentLoaded', async () => {
    showErrorFromQuery();
    await loadCart();
});

async function loadCart() {
    const container = document.getElementById('cartItems');
    const summary = document.getElementById('cartSummary');

    try {
        const res = await fetch('cart?action=view');
        if (res.status === 401) {
            window.location.href = 'login.html';
            return;
        }
        const items = await res.json();

        if (items.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <p>Your cart is empty.</p>
                    <br>
                    <a href="products.html" class="btn-secondary">Browse Products</a>
                </div>`;
            summary.style.display = 'none';
            return;
        }

        let total = 0;
        container.innerHTML = items.map(item => {
            const subtotal = item.price * item.quantity;
            total += subtotal;
            return `
                <div class="cart-item">
                    <img src="${item.imageUrl || 'https://via.placeholder.com/80x80?text=Item'}" alt="${escapeHtml(item.name)}">
                    <div class="details">
                        <div class="name">${escapeHtml(item.name)}</div>
                        <div class="qty">Qty: ${item.quantity} × ₹${Number(item.price).toLocaleString('en-IN')}</div>
                    </div>
                    <div class="subtotal">₹${subtotal.toLocaleString('en-IN')}</div>
                    <button class="remove-btn" onclick="removeItem(${item.cartItemId})">Remove</button>
                </div>
            `;
        }).join('');

        document.getElementById('totalAmount').textContent = '₹' + total.toLocaleString('en-IN');
        summary.style.display = 'block';

    } catch (e) {
        console.error(e);
        container.innerHTML = '<p>Failed to load cart. Please refresh.</p>';
    }
}

async function removeItem(cartItemId) {
    try {
        const body = new URLSearchParams();
        body.append('cartItemId', cartItemId);

        await fetch('cart?action=remove', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body.toString()
        });
        await loadCart();
    } catch (e) {
        console.error(e);
    }
}

function showErrorFromQuery() {
    const params = new URLSearchParams(window.location.search);
    const errorBox = document.getElementById('errorBox');
    const errors = {
        insufficient_stock: 'One or more items in your cart exceed available stock.',
        empty_cart: 'Your cart is empty.',
        server_error: 'Something went wrong while placing your order. Please try again.'
    };
    if (params.get('error') && errors[params.get('error')]) {
        errorBox.textContent = errors[params.get('error')];
        errorBox.style.display = 'block';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
