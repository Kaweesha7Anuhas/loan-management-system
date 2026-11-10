// api.js

// Read the CSRF token from the cookie
function getCsrfToken() {
    const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
    if (match) return decodeURIComponent(match[2]);
    return null;
}

// Configure jQuery globally
$.ajaxSetup({
    beforeSend: function(xhr) {
        const token = getCsrfToken();
        if (token) {
            xhr.setRequestHeader("X-XSRF-TOKEN", token);
        }
    }
});

// Global AJAX error handler for unauthenticated requests
$(document).ajaxError(function(event, jqXHR, ajaxSettings, thrownError) {
    if (jqXHR.status === 401 && window.location.pathname !== '/login.html') {
        window.location.href = '/login.html';
    }
});

// Helper function to format currency
function formatCurrency(amount) {
    if (!amount) return "LKR 0.00";
    return "LKR " + parseFloat(amount).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}