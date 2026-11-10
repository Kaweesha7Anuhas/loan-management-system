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
                $('#link-users').hide();
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
    $('#view-users').hide();
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
    } else if (page === 'users') {
        $('#page-title').text('User Management');
        $('#view-users').fadeIn();
        loadUsers();
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
            $('#lbl-card-1').text('Total Customers');
            $('#stat-customers').text(data.totalCustomers);
            $('#lbl-card-2').text('Active Loans');
            $('#stat-active-loans').text(data.activeLoans);
            $('#lbl-card-3').text('Total Outstanding');
            $('#stat-outstanding').text(formatCurrency(data.totalOutstandingBalance));
            $('#lbl-card-4').text('Total Collected');
            $('#stat-collected').text(formatCurrency(data.totalRepaymentsCollected));
            
        } else if (role === 'LOAN_OFFICER') {
            // Officer sees application statistics
            $('#lbl-card-1').text('Total Applications');
            $('#stat-customers').text(data.totalApplicationsCreated);
            $('#lbl-card-2').text('Under Review');
            $('#stat-active-loans').text(data.underReviewApplications);
            $('#lbl-card-3').text('Recommended');
            $('#stat-outstanding').text(data.recommendedApplications);
            $('#lbl-card-4').text('Approved');
            $('#stat-collected').text(data.approvedApplications);
            
        } else if (role === 'CASHIER') {
            // Cashier sees collection statistics
            $('#lbl-card-1').text('Collected Today');
            $('#stat-customers').text(formatCurrency(data.repaymentsToday));
            $('#lbl-card-2').text('Collected This Month');
            $('#stat-active-loans').text(formatCurrency(data.repaymentsThisMonth));
            $('#lbl-card-3').text('Active Loans');
            $('#stat-outstanding').text(data.activeLoans);
            $('#lbl-card-4').text('Completed Loans');
            $('#stat-collected').text(data.completedLoans);
        }
    });
}