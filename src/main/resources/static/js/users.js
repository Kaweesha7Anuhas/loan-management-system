function loadUsers() {
    $('#userTableBody').html('<tr><td colspan="6" class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></td></tr>');

    $.get('/api/users', function(res) {
        let html = '';
        res.data.forEach(u => {
            const badge = u.enabled ? 'bg-success' : 'bg-danger';
            const statusText = u.enabled ? 'ACTIVE' : 'DISABLED';
            const roleName = u.roles[0] || 'NONE';

            html += `
                <tr>
                    <td class="ps-4 fw-bold">${u.fullName}</td>
                    <td>${u.username}</td>
                    <td>${u.email}</td>
                    <td><span class="badge bg-secondary">${roleName}</span></td>
                    <td><span class="badge ${badge} rounded-pill px-3">${statusText}</span></td>
                    <td class="text-end pe-4">
                        <button class="btn btn-sm ${u.enabled ? 'btn-outline-danger' : 'btn-outline-success'}" onclick="toggleUser(${u.id})">
                            ${u.enabled ? 'Disable' : 'Enable'}
                        </button>
                    </td>
                </tr>
            `;
        });
        $('#userTableBody').html(html);
    });
}

function clearUserForm() {
    $('#userForm')[0].reset();
}

function saveUser() {
    const payload = {
        fullName: $('#userFullName').val(),
        username: $('#userUsername').val(),
        email: $('#userEmail').val(),
        password: $('#userPassword').val(),
        roleName: $('#userRole').val()
    };

    $('#saveUserBtn').prop('disabled', true).text('Creating...');

    $.ajax({
        url: '/api/users',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function() {
            $('#userModal').modal('hide');
            $('#saveUserBtn').prop('disabled', false).text('Create User');
            loadUsers();
        },
        error: function(xhr) {
            $('#saveUserBtn').prop('disabled', false).text('Create User');
            alert(xhr.responseJSON ? xhr.responseJSON.message : 'Error creating user');
        }
    });
}

function toggleUser(id) {
    if (!confirm("Are you sure you want to change this user's status?")) return;
    
    $.ajax({
        url: `/api/users/${id}/status`,
        method: 'PATCH',
        success: function() {
            loadUsers();
        },
        error: function(xhr) {
            alert(xhr.responseJSON ? xhr.responseJSON.message : 'Error updating status');
        }
    });
}