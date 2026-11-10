window.currentUserRole = null;

$(document).ready(function() {
    $.get('/api/auth/me')
        .done(function(res) {
            const user = res.data;
            window.currentUserRole = user.roles[0];
            
            $('#user-name').text(user.fullName + ' (' + user.roles[0] + ')');
            $('#user-avatar').text(user.fullName.charAt(0).toUpperCase());
            loadDashboardStats(user.roles[0]);
            
            if (user.roles[0] === 'CASHIER') {
                $('#btnNewApp').hide();
            }
            
            // Hide Admin-only links
            if (user.roles[0] !== 'ADMIN') {
                $('#link-loan-types').hide();
                $('#link-reports').hide();
                $('#link-audit').hide();
            }
        })
        .fail(function() { window.location.href = '/login.html'; });

    $('#logoutBtn').on('click', function(e) {
        e.preventDefault();
        $.post('/api/auth/logout', function() { window.location.href = '/login.html'; });
    });
});

function loadPage(page) {
    $('#sidebar-wrapper .list-group-item').removeClass('active');
    $(`#sidebar-wrapper .list-group-item[onclick="loadPage('${page}')"]`).addClass('active');
    
    $('#view-dashboard').hide();
    $('#view-customers').hide();
    $('#view-applications').hide();
    $('#view-loans').hide();
    $('#view-loan-details').hide();
    $('#view-loan-types').hide();
    $('#view-reports').hide();
    $('#view-audit-logs').hide();
    
    if (page === 'dashboard') {
        $('#page-title').text('Dashboard Overview');
        $('#view-dashboard').fadeIn();
        loadDashboardStats(window.currentUserRole || 'ADMIN'); 
    } else if (page === 'customers') {
        $('#page-title').text('Customer Management');
        $('#view-customers').fadeIn();
        loadCustomers(0, ''); 
    } else if (page === 'applications') {
        $('#page-title').text('Loan Applications');
        $('#view-applications').fadeIn();
        loadApplications(0, '');
    } else if (page === 'loans') {
        $('#page-title').text('Active Loans');
        $('#view-loans').fadeIn();
        loadLoans(0, '');
    } else if (page === 'loan-types') {
        $('#page-title').text('Loan Products');
        $('#view-loan-types').fadeIn();
        loadLoanTypes();
    } else if (page === 'reports') {
        $('#page-title').text('Financial Reports');
        $('#view-reports').fadeIn();
        loadReports();
    } else if (page === 'audit-logs') {
        $('#page-title').text('System Audit Logs');
        $('#view-audit-logs').fadeIn();
        loadAuditLogs();
    }
}

function loadDashboardStats(role) {
    let endpoint = '/api/dashboard/' + (role === 'ADMIN' ? 'admin' : role === 'CASHIER' ? 'cashier' : 'officer');
    $.get(endpoint).done(function(res) {
        const data = res.data;
        if (role === 'ADMIN') {
            $('#stat-customers').text(data.totalCustomers);
            $('#stat-active-loans').text(data.activeLoans);
            $('#stat-outstanding').text(formatCurrency(data.totalOutstandingBalance));
            $('#stat-collected').text(formatCurrency(data.totalRepaymentsCollected));
        } else if (role === 'CASHIER') {
            $('#stat-customers').text(data.activeLoans); 
            $('#stat-active-loans').text(data.completedLoans);
            $('#stat-outstanding').text(formatCurrency(data.repaymentsToday));
            $('#stat-collected').text(formatCurrency(data.repaymentsThisMonth));
        }
    });
}