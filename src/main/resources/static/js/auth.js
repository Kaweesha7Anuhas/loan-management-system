// auth.js
// Login-page controller. Handles sign-in and "already authenticated" redirect.
// Loaded ONLY on login.html. Depends on api.js (CSRF + global 401 handling).

'use strict';

$(function () {
    /* ---------------------------------------------------------------------
     * Auto-redirect: if the user already has a valid session, skip login.
     * Use replace() so the login page does not end up in Back history.
     * ------------------------------------------------------------------- */
    $.get('/api/auth/me')
        .done(function () {
            window.location.replace('/index.html');
        })
        .fail(function () {
            // Not authenticated — stay here. Focus the first field for
            // keyboard-first users (tellers / CSRs mostly log in fast).
            $('#username').trigger('focus');
        });

    /* ---------------------------------------------------------------------
     * Sign-in form
     * ------------------------------------------------------------------- */
    var submitting = false;

    $('#loginForm').on('submit', function (e) {
        e.preventDefault();
        if (submitting) return;
        submitting = true;

        var $btn = $('#loginBtn');
        var $err = $('#loginError');

        $btn.prop('disabled', true).text('Signing in...');
        $err.addClass('d-none').text('');

        var payload = {
            username: ($('#username').val() || '').trim(),
            password: $('#password').val() || ''
        };

        // Client-side guard: never send an empty credential request.
        if (!payload.username || !payload.password) {
            $btn.prop('disabled', false).text('Sign In');
            $err.text('Please enter both username and password.').removeClass('d-none');
            submitting = false;
            return;
        }

        $.ajax({
            url: '/api/auth/login',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload)
        })
            .done(function () {
                // Session cookie is set by the server; nothing to store client-side.
                // replace() so the Back button does not bounce via /login.html.
                window.location.replace('/index.html');
            })
            .fail(function (xhr) {
                $btn.prop('disabled', false).text('Sign In');
                submitting = false;

                var msg;
                if (xhr && xhr.status === 0) {
                    msg = 'Cannot reach the server. Please check your connection.';
                } else if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    msg = xhr.responseJSON.message;
                } else if (xhr && xhr.status === 401) {
                    msg = 'Invalid username or password.';
                } else if (xhr && xhr.status >= 500) {
                    msg = 'Server error. Please try again later.';
                } else {
                    msg = 'Invalid credentials';
                }

                $err.text(msg).removeClass('d-none');
                // Clear the password so the user can retype immediately.
                $('#password').val('').trigger('focus');
            });
    });
});