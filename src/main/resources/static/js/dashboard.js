// dashboard.js
// App shell controller:
//   - Session bootstrap (GET /api/auth/me) → sets window.currentUserRole,
//     populates #user-name / #user-avatar, hides role-specific UI.
//   - SPA router: global loadPage(page).
//   - Role-specific dashboard stats: loadDashboardStats(role).
//   - Admin Charts: renderAdminCharts(data).
//   - Logout: POST /api/auth/logout → redirect.

'use strict';

window.currentUserRole = null;

// Chart instances (held globally so they can be destroyed and redrawn)
let portfolioChartInstance = null;
let statusChartInstance = null;

/* ──────────────────────────────────────────────────────────────────────
 * Session bootstrap
 * ──────────────────────────────────────────────────────────────────── */

$(function () {
    $.get('/api/auth/me')
        .done(function (res) {
            const user = (res && res.data) || null;

            if (!user) {
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

            // Admin-only UI elements
            if (role !== 'ADMIN') {
                $('#link-users').hide();
                $('#link-loan-types').hide();
                $('#link-reports').hide();
                $('#link-audit').hide();
                $('#adminChartsRow').hide(); // Hide charts for non-admins
            } else {
                $('#adminChartsRow').show(); // Show charts for admins
            }
        })
        .fail(function () {
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
 * Dashboard stats (role-specific) & Charts
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

                // Render Charts
                renderAdminCharts(data);

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

function renderAdminCharts(data) {
    if (!document.getElementById('portfolioChart') || !document.getElementById('statusChart')) return;

    // --- 1. Portfolio Chart (Bar) ---
    if (portfolioChartInstance) {
        portfolioChartInstance.destroy(); 
    }
    
    const ctxPortfolio = document.getElementById('portfolioChart').getContext('2d');
    portfolioChartInstance = new Chart(ctxPortfolio, {
        type: 'bar',
        data: {
            labels: ['Disbursed', 'Interest Exp.', 'Outstanding', 'Collected'],
            datasets: [{
                label: 'Amount (LKR)',
                data: [
                    data.totalDisbursedPrincipal || 0,
                    data.totalInterestExpected || 0,
                    data.totalOutstandingBalance || 0,
                    data.totalRepaymentsCollected || 0
                ],
                backgroundColor: [
                    'rgba(37, 99, 235, 0.8)',
                    'rgba(22, 163, 74, 0.8)',
                    'rgba(220, 38, 38, 0.8)',
                    'rgba(8, 145, 178, 0.8)'
                ],
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { borderDash: [4, 4] },
                    ticks: {
                        callback: function(value) {
                            return 'LKR ' + value.toLocaleString();
                        }
                    }
                },
                x: { grid: { display: false } }
            }
        }
    });

    // --- 2. Status Chart (Doughnut) ---
    if (statusChartInstance) {
        statusChartInstance.destroy();
    }

    const ctxStatus = document.getElementById('statusChart').getContext('2d');
    statusChartInstance = new Chart(ctxStatus, {
        type: 'doughnut',
        data: {
            labels: ['Active Loans', 'Completed Loans'],
            datasets: [{
                data: [
                    data.activeLoans || 0, 
                    data.completedLoans || 0 
                ],
                backgroundColor: ['#16A34A', '#64748B'],
                borderWidth: 0,
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '70%',
            plugins: {
                legend: { position: 'bottom', labels: { usePointStyle: true, padding: 20 } }
            }
        }
    });
}