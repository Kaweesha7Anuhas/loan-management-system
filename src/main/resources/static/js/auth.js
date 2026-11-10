$(document).ready(function() {
    // If already logged in, go to dashboard
    $.get('/api/auth/me')
        .done(function() { window.location.href = '/index.html'; })
        .fail(function() { /* Not logged in, stay on page */ });

    $('#loginForm').on('submit', function(e) {
        e.preventDefault();
        
        const btn = $('#loginBtn');
        btn.prop('disabled', true).text('Signing in...');
        $('#loginError').addClass('d-none');

        const payload = {
            username: $('#username').val(),
            password: $('#password').val()
        };

        $.ajax({
            url: '/api/auth/login',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function(response) {
                window.location.href = '/index.html';
            },
            error: function(xhr) {
                btn.prop('disabled', false).text('Sign In');
                const msg = xhr.responseJSON ? xhr.responseJSON.message : 'Invalid credentials';
                $('#loginError').text(msg).removeClass('d-none');
            }
        });
    });
});