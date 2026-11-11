// users.js
// System Users management: listing + create + enable/disable toggle.
// GET /api/users returns a plain array (not a paginated Page object), so
// there is no pagination here — by design, matching the backend.
// Depends on: api.js (jQuery setup, CSRF), Bootstrap 5 modals.

'use strict';

const USER_COLSPAN = 6;

// Cache of the last-loaded users, used only to enrich the confirm() dialog.
// Never used as a source of truth for rendering.
let usersListCache = [];

/* ──────────────────────────────────────────────────────────────────────
 * Helpers
 * ──────────────────────────────────────────────────────────────────── */

function escapeUserHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function openUserModal() {
    var el = document.getElementById('userModal');
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).show();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) $(el).modal('show');
}

function closeUserModal() {
    var el = document.getElementById('userModal');
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).hide();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) $(el).modal('hide');
}

function userRoleClass(role) {
    if (role === 'ADMIN')        return 'status-primary';
    if (role === 'LOAN_OFFICER') return 'status-info';
    if (role === 'CASHIER')      return 'status-warning';
    return 'status-neutral';
}

function findCachedUser(id) {
    for (var i = 0; i < usersListCache.length; i++) {
        if (String(usersListCache[i].id) === String(id)) return usersListCache[i];
    }
    return null;
}

/* ──────────────────────────────────────────────────────────────────────
 * List
 * ──────────────────────────────────────────────────────────────────── */

function loadUsers() {
    $('#userTableBody').html(
        `<tr><td colspan="${USER_COLSPAN}" class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
        </td></tr>`
    );

    $.get('/api/users')
        .done(function (res) {
            const users = (res && Array.isArray(res.data)) ? res.data : [];
            usersListCache = users;

            let html = '';

            if (!users.length) {
                html = `<tr><td colspan="${USER_COLSPAN}" class="border-0">
                    <div class="empty-state">
                        <i class="bi bi-person-badge"></i>
                        No system users found
                    </div>
                </td></tr>`;
            } else {
                users.forEach(u => {
                    const enabled = !!u.enabled;
                    const statusCls = enabled ? 'status-success' : 'status-danger';
                    const statusText = enabled ? 'ACTIVE' : 'DISABLED';

                    const roleName = (Array.isArray(u.roles) && u.roles.length)
                        ? u.roles[0]
                        : 'NONE';

                    const toggleBtnCls = enabled
                        ? 'btn-outline-danger'
                        : 'btn-outline-success';
                    const toggleLabel  = enabled ? 'Disable' : 'Enable';
                    const toggleTitle  = enabled
                        ? 'Disable this user account'
                        : 'Enable this user account';

                    html += `
                        <tr>
                            <td class="ps-4 fw-semibold">${escapeUserHtml(u.fullName)}</td>
                            <td>${escapeUserHtml(u.username)}</td>
                            <td>${escapeUserHtml(u.email)}</td>
                            <td>
                                <span class="status-badge ${userRoleClass(roleName)}">${escapeUserHtml(roleName)}</span>
                            </td>
                            <td>
                                <span class="status-badge ${statusCls}">${statusText}</span>
                            </td>
                            <td class="text-end pe-4">
                                <button class="btn btn-sm ${toggleBtnCls}"
                                        title="${toggleTitle}"
                                        onclick="toggleUser(${Number(u.id)})">
                                    ${toggleLabel}
                                </button>
                            </td>
                        </tr>
                    `;
                });
            }
            $('#userTableBody').html(html);
        })
        .fail(function () {
            usersListCache = [];
            $('#userTableBody').html(
                `<tr><td colspan="${USER_COLSPAN}" class="text-center py-5 text-danger">
                    Failed to load users
                </td></tr>`
            );
        });
}

/* ──────────────────────────────────────────────────────────────────────
 * Form: clear + create
 * ──────────────────────────────────────────────────────────────────── */

function clearUserForm() {
    const form = document.getElementById('userForm');
    if (form) form.reset();
}

// Keyboard-friendly: Enter inside the form triggers save.
$(function () {
    $('#userForm').on('submit', function (e) {
        e.preventDefault();
        saveUser();
    });
});

function saveUser() {
    // 1. Native HTML5 constraint validation (required fields, email format).
    //    NOTE: password is deliberately not trimmed or length-checked —
    //    whatever the browser/user supplies goes to the backend unchanged.
    const form = document.getElementById('userForm');
    if (form && typeof form.reportValidity === 'function' && !form.checkValidity()) {
        form.reportValidity();
        return;
    }

    // 2. Build the payload — same keys, same order as the original.
    const payload = {
        fullName: $('#userFullName').val(),
        username: $('#userUsername').val(),
        email: $('#userEmail').val(),
        password: $('#userPassword').val(),
        roleName: $('#userRole').val()
    };

    const $btn = $('#saveUserBtn').prop('disabled', true).text('Creating...');

    $.ajax({
        url: '/api/users',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload)
    })
        .done(function () {
            closeUserModal();
            $btn.prop('disabled', false).text('Create User');
            clearUserForm(); // wipe password from the DOM
            loadUsers();
        })
        .fail(function (xhr) {
            $btn.prop('disabled', false).text('Create User');
            const msg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : 'Error creating user';
            alert(msg);
        });
}

/* ──────────────────────────────────────────────────────────────────────
 * Enable / disable toggle
 * ──────────────────────────────────────────────────────────────────── */

function toggleUser(id) {
    const cached = findCachedUser(id);
    const label = cached && cached.username
        ? 'user "' + cached.username + '"'
        : 'this user';
    const action = cached
        ? (cached.enabled ? 'Disable' : 'Enable')
        : 'change the status of';

    if (!confirm('Are you sure you want to ' + action.toLowerCase() + ' ' + label + '?')) {
        return;
    }

    // PATCH with NO body and NO contentType — matching the endpoint contract.
    $.ajax({
        url: `/api/users/${id}/status`,
        method: 'PATCH'
    })
        .done(function () {
            loadUsers();
        })
        .fail(function (xhr) {
            const msg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : 'Error updating status';
            alert(msg);
        });
}