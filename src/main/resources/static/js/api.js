// api.js
// Shared API bootstrap: CSRF handling, global 401 redirect, and formatting helpers.
// Loaded FIRST (before any feature script), so it must stay dependency-free.

'use strict';

/* --------------------------------------------------------------------------
 * CSRF token
 * ------------------------------------------------------------------------ */

/**
 * Read the XSRF-TOKEN cookie value.
 * Returns null when the cookie is absent.
 */
function getCsrfToken() {
    var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : null;
}

// Attach the CSRF token to every jQuery AJAX request.
$.ajaxSetup({
    beforeSend: function (xhr) {
        var token = getCsrfToken();
        if (token) {
            xhr.setRequestHeader('X-XSRF-TOKEN', token);
        }
    }
});

/* --------------------------------------------------------------------------
 * Global 401 handling
 * ------------------------------------------------------------------------ */

// Single-flight guard: concurrent 401s must not trigger multiple redirects.
var __lmsRedirecting = false;

function __isLoginPath(pathname) {
    return /(?:^|\/)login(?:\.html)?\/?$/i.test(pathname || '');
}

// Redirect to login whenever the server rejects the session.
$(document).ajaxError(function (event, jqXHR) {
    if (!jqXHR || jqXHR.status !== 401) return;
    if (__lmsRedirecting) return;
    if (__isLoginPath(window.location.pathname)) return;

    __lmsRedirecting = true;
    // replace() prevents the "back" button from re-entering a protected page.
    window.location.replace('/login.html');
});

/* --------------------------------------------------------------------------
 * Formatting helpers
 * ------------------------------------------------------------------------ */

/**
 * Format an amount as Sri Lankan Rupees, e.g. "LKR 12,345.67".
 * Any non-numeric / nullish input resolves to "LKR 0.00".
 *
 * @param {number|string|null|undefined} amount
 * @returns {string}
 */
function formatCurrency(amount) {
    var value = (amount === null || amount === undefined || amount === '')
        ? 0
        : parseFloat(amount);

    if (!isFinite(value)) {
        value = 0;
    }

    return 'LKR ' + value.toLocaleString('en-LK', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}