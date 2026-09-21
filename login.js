function selectRole(role) {
    document.getElementById('roleInput').value = role;

    const tabBuyer = document.getElementById('tabBuyer');
    const tabSeller = document.getElementById('tabSeller');
    const submitBtn = document.getElementById('submitBtn');

    if (role === 'BUYER') {
        tabBuyer.classList.add('active');
        tabSeller.classList.remove('active');
        submitBtn.textContent = 'Login as Buyer';
    } else {
        tabSeller.classList.add('active');
        tabBuyer.classList.remove('active');
        submitBtn.textContent = 'Login as Seller';
    }
}

// Show messages based on query params (?error=... or ?registered=true)
window.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const errorBox = document.getElementById('errorBox');
    const successBox = document.getElementById('successBox');

    const errors = {
        invalid_credentials: 'Invalid email, password, or role. Please try again.',
        server_error: 'Something went wrong. Please try again later.'
    };

    if (params.get('error') && errors[params.get('error')]) {
        errorBox.textContent = errors[params.get('error')];
        errorBox.style.display = 'block';
    }

    if (params.get('registered') === 'true') {
        successBox.textContent = 'Registration successful! Please log in.';
        successBox.style.display = 'block';
    }
});
