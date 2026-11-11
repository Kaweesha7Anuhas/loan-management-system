// dashboard.js
// App shell controller:
//   - Session bootstrap (GET /api/auth/me) → sets window.currentUserRole,
//     populates #user-name / #user-avatar, hides role-specific UI.
//   - SPA router: global loadPage(page).
//   - Role-specific dashboard stats: loadDashboardStats(role).
//   - Logout: POST /api/auth/logout → redirect.
//
// Loaded FIRST on index.html; every feature file depends on this being
// initialized before they are used. Depends on api.js (jQuery setup + CSRF).

'use strict';

window.currentUserRole = null;

/* ──────────────────────────────────────────────────────────────────────
 * Session bootstrap
 * ──────────────────────────────────────────────────────────────────── */

$(function () {
    $.get('/api/auth/me')
        .done(function (res) {
            const user = (res && res.data) || null;

            if (!user) {
                // Malformed but authenticated response — bail to login.
                window.location.replace('/login.html');
                return;
            }

            const role = (Array.isArray(user.roles) && user.roles.length)
                ? user.roles[0]
                : null;

            window.currentUserRole = role;

            // Top bar identity (safe: .text() escapes any backend value)
            const fullName = user.fullName || 'User';
            const roleLabel = role || 'USER';
            $('#user-name').text(fullName + ' (' + roleLabel + ')');
            $('#user-avatar').text((fullName.charAt(0) || '?').toUpperCase());

            // Dashboard stats for this role
            if (role) {
                loadDashboardStats(role);
            }

            // CASHIER cannot create applications
            if (role === 'CASHIER') {
                $('#btnNewApp').hide();
            }

            // Admin-only sidebar links
            if (role !== 'ADMIN') {
                $('#link-users').hide();
                $('#link-loan-types').hide();
                $('#link-reports').hide();
                $('#link-audit').hide();
            }
        })
        .fail(function () {
            // Unauthenticated → login. replace() so Back button doesn't
            // bounce user back into a protected page.
            window.location.replace('/login.html');
        });

    /* ──────────────────────────────────────────────────────────────
     * Logout
     * ──────────────────────────────────────────────────────────── */
    var loggingOut = false;
    $('#logoutBtn').on('click', function (e) {
        e.preventDefault();
        if (loggingOut) return;
        loggingOut = true;

        var $btn = $(this);
        $btn.addClass('disabled');

        $.post('/api/auth/logout')
            .done(function () {
                window.location.replace('/login.html');
            })
            .fail(function (xhr) {
                loggingOut = false;
                $btn.removeClass('disabled');
                var msg = (xhr.responseJSON && xhr.responseJSON.message)
                    || 'Could not sign out. Please try again.';
                alert(msg);
            });
    });
});

/* ──────────────────────────────────────────────────────────────────────
 * SPA router
 * ──────────────────────────────────────────────────────────────────── */

// Pages that render a list of data. Missing a loader (e.g. script file
// failed to load) results in a visible error rather than a blank page.
function showLoadError(label) {
    var colspans = {
        customers: 7,
        applications: 5,
        loans: 6,
        'loan-types': 7,
        users: 6,
        'audit-logs': 5
    };
    var map = {
        customers: '#customerTableBody',
        applications: '#appTableBody',
        loans: '#loanTableBody',
        'loan-types': '#loanTypeTableBody',
        users: '#userTableBody',
        'audit-logs': '#auditTableBody'
    };
    var selector = map[label];
    if (!selector) return;
    var span = colspans[label] || 6;
    $(selector).html(
        '<tr><td colspan="' + span + '" class="text-center py-5 text-danger">' +
            'Could not load ' + label + ' (script failed to load)' +
        '</td></tr>'
    );
}

// Mark the corresponding sidebar link as active.
// Handles both data-page="…" (future) and onclick="loadPage('…')" (current).
function setActiveSidebarLink(page) {
    var $items = $('#sidebar-wrapper .list-group-item');
    $items.removeClass('active');

    var matched = $items.filter(function () {
        var dataPage = this.getAttribute('data-page');
        if (dataPage) return dataPage === page;
        var onclick = this.getAttribute('onclick') || '';
        return onclick.indexOf("loadPage('" + page + "')") !== -1;
    });

    if (matched.length) {
        matched.addClass('active');
    }
}

function loadPage(page) {
    setActiveSidebarLink(page);

    // Hide every known view
    $('#view-dashboard').hide();
    $('#view-customers').hide();
    $('#view-applications').hide();
    $('#view-loans').hide();
    $('#view-loan-details').hide();
    $('#view-loan-types').hide();
    $('#view-users').hide();
    $('#view-reports').hide();
    $('#view-audit-logs').hide();

    if (page === 'dashboard') {
        $('#page-title').text('Dashboard Overview');
        $('#view-dashboard').fadeIn();
        if (window.currentUserRole) {
            loadDashboardStats(window.currentUserRole);
        } else {
            // Very-rare race: page clicked before /api/auth/me resolved.
            // Keep behaviour compatible with the original (default to ADMIN
            // — the server still enforces auth, so no data is exposed).
            console.warn('[loadPage] currentUserRole not set yet; defaulting to ADMIN for stats.');
            loadDashboardStats('ADMIN');
        }
        return;
    }

    if (page === 'customers') {
        $('#page-title').text('Customer Management');
        $('#view-customers').fadeIn();
        if (typeof loadCustomers === 'function') loadCustomers(0, '');
        else showLoadError('customers');
        return;
    }

    if (page === 'applications') {
        $('#page-title').text('Loan Applications');
        $('#view-applications').fadeIn();
        if (typeof loadApplications === 'function') loadApplications(0, '');
        else showLoadError('applications');
        return;
    }

    if (page === 'loans') {
        $('#page-title').text('Active Loans');
        $('#view-loans').fadeIn();
        if (typeof loadLoans === 'function') loadLoans(0, '');
        else showLoadError('loans');
        return;
    }

    if (page === 'loan-types') {
        $('#page-title').text('Loan Products');
        $('#view-loan-types').fadeIn();
        if (typeof loadLoanTypes === 'function') loadLoanTypes();
        else showLoadError('loan-types');
        return;
    }

    if (page === 'users') {
        $('#page-title').text('User Management');
        $('#view-users').fadeIn();
        if (typeof loadUsers === 'function') loadUsers();
        else showLoadError('users');
        return;
    }

    if (page === 'reports') {
        $('#page-title').text('Financial Reports');
        $('#view-reports').fadeIn();
        if (typeof loadReports === 'function') loadReports();
        else showLoadError('reports');
        return;
    }

    if (page === 'audit-logs') {
        $('#page-title').text('System Audit Logs');
        $('#view-audit-logs').fadeIn();
        if (typeof loadAuditLogs === 'function') loadAuditLogs();
        else showLoadError('audit-logs');
        return;
    }

    console.warn('[loadPage] Unknown page:', page);
}

/* ──────────────────────────────────────────────────────────────────────
 * Dashboard stats (role-specific)
 * ──────────────────────────────────────────────────────────────────── */

function loadDashboardStats(role) {
    var endpoint = '/api/dashboard/' + (
        role === 'ADMIN'   ? 'admin'   :
        role === 'CASHIER' ? 'cashier' :
                             'officer'
    );

    // Brief "refreshing" indicator so stale numbers are not mistaken for
    // current ones.
    $('#stat-customers, #stat-active-loans, #stat-outstanding, #stat-collected')
        .text('—');

    $.get(endpoint)
        .done(function (res) {
            const data = (res && res.data) || {};

            if (role === 'ADMIN') {
                $('#lbl-card-1').text('Total Customers');
                $('#stat-customers').text(data.totalCustomers != null ? data.totalCustomers : '—');
                $('#lbl-card-2').text('Active Loans');
                $('#stat-active-loans').text(data.activeLoans != null ? data.activeLoans : '—');
                $('#lbl-card-3').text('Total Outstanding');
                $('#stat-outstanding').text(formatCurrency(data.totalOutstandingBalance));
                $('#lbl-card-4').text('Total Collected');
                $('#stat-collected').text(formatCurrency(data.totalRepaymentsCollected));

            } else if (role === 'LOAN_OFFICER') {
                $('#lbl-card-1').text('Total Applications');
                $('#stat-customers').text(data.totalApplicationsCreated != null ? data.totalApplicationsCreated : '—');
                $('#lbl-card-2').text('Under Review');
                $('#stat-active-loans').text(data.underReviewApplications != null ? data.underReviewApplications : '—');
                $('#lbl-card-3').text('Recommended');
                $('#stat-outstanding').text(data.recommendedApplications != null ? data.recommendedApplications : '—');
                $('#lbl-card-4').text('Approved');
                $('#stat-collected').text(data.approvedApplications != null ? data.approvedApplications : '—');

            } else if (role === 'CASHIER') {
                $('#lbl-card-1').text('Collected Today');
                $('#stat-customers').text(formatCurrency(data.repaymentsToday));
                $('#lbl-card-2').text('Collected This Month');
                $('#stat-active-loans').text(formatCurrency(data.repaymentsThisMonth));
                $('#lbl-card-3').text('Active Loans');
                $('#stat-outstanding').text(data.activeLoans != null ? data.activeLoans : '—');
                $('#lbl-card-4').text('Completed Loans');
                $('#stat-collected').text(data.completedLoans != null ? data.completedLoans : '—');
            }
        })
        .fail(function () {
            // Visible failure state so users aren't reading stale numbers.
            $('#stat-customers, #stat-active-loans, #stat-outstanding, #stat-collected')
                .text('—');
            $('#lbl-card-1, #lbl-card-2, #lbl-card-3, #lbl-card-4')
                .each(function () {
                    if (!/unavailable/i.test($(this).text())) {
                        $(this).text($(this).text() + ' (unavailable)');
                    }
                });
        });
}