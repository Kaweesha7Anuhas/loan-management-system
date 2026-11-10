let currentAppPage = 0;
let currentAppSearch = '';
let appSearchTimeout = null;

// ─────────────────────────────────────────────
// LOAD AND RENDER TABLE
// ─────────────────────────────────────────────
function loadApplications(page, search) {
    currentAppPage = page;
    currentAppSearch = search;

    $('#appTableBody').html('<tr><td colspan="5" class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></td></tr>');

    $.get(`/api/loan-applications?page=${page}&size=10&search=${search}&sortBy=createdAt&sortDir=desc`)
        .done(function(res) {
            const data = res.data;
            let html = '';

            if (data.empty) {
                html = '<tr><td colspan="5" class="text-center py-5 text-muted">No applications found</td></tr>';
            } else {
                data.content.forEach(a => {
                    let badgeClass = 'bg-secondary';
                    if (a.applicationStatus === 'APPROVED') badgeClass = 'bg-success';
                    else if (a.applicationStatus === 'REJECTED') badgeClass = 'bg-danger';
                    else if (a.applicationStatus === 'SUBMITTED' || a.applicationStatus === 'UNDER_REVIEW') badgeClass = 'bg-warning text-dark';
                    else if (a.applicationStatus === 'RECOMMENDED') badgeClass = 'bg-info text-dark';
                    else if (a.applicationStatus === 'DRAFT') badgeClass = 'bg-dark';

                    // Dynamic Action Buttons based on Status and Role
                    let actionHtml = '';
                    const role = window.currentUserRole;

                    if (a.applicationStatus === 'DRAFT') {
                        actionHtml = `<button class="btn btn-sm btn-primary" onclick="submitApp(${a.id})">Submit</button>`;
                    } else if (a.applicationStatus === 'SUBMITTED') {
                        actionHtml = `<button class="btn btn-sm btn-warning text-dark" onclick="reviewApp(${a.id})">Start Review</button>`;
                    } else if (a.applicationStatus === 'UNDER_REVIEW') {
                        actionHtml = `<button class="btn btn-sm btn-info text-dark" onclick="openRecommendModal(${a.id})">Recommend</button>`;
                    } else if (a.applicationStatus === 'RECOMMENDED') {
                        if (role === 'ADMIN') {
                            actionHtml = `
                                <button class="btn btn-sm btn-success" onclick="openDecideModal(${a.id}, 'APPROVED')">Approve</button>
                                <button class="btn btn-sm btn-danger ms-1" onclick="openDecideModal(${a.id}, 'REJECTED')">Reject</button>
                            `;
                        } else {
                            actionHtml = `<span class="text-muted small">Awaiting Admin</span>`;
                        }
                    } else if (a.applicationStatus === 'APPROVED') {
                        if (role === 'ADMIN') {
                            if (a.disbursed) { // <-- NEW CHECK
                                actionHtml = `<button class="btn btn-sm btn-outline-primary" onclick="loadPage('loans')">View Loan</button>`;
                            } else {
                                actionHtml = `<button class="btn btn-sm btn-dark" onclick="disburseApp(${a.id})">Disburse</button>`;
                            }
                        } else {
                            actionHtml = `<span class="text-muted small">Approved</span>`;
                        }
                    } else {
                        actionHtml = `<span class="text-muted small">No actions</span>`;
                    }

                    html += `
                        <tr>
                            <td class="ps-4 fw-bold text-primary">${a.applicationNumber}</td>
                            <td>
                                <div class="fw-bold">${a.customerFullName}</div>
                                <div class="small text-muted">${a.loanTypeName}</div>
                            </td>
                            <td>${formatCurrency(a.requestedAmount)}</td>
                            <td><span class="badge ${badgeClass} rounded-pill px-3">${a.applicationStatus}</span></td>
                            <td class="text-end pe-4">${actionHtml}</td>
                        </tr>
                    `;
                });
            }
            $('#appTableBody').html(html);
            renderAppPagination(data);
        });
}

function renderAppPagination(pageData) {
    $('#appPaginationInfo').text(`Showing ${pageData.empty ? 0 : pageData.page * pageData.size + 1} to ${Math.min((pageData.page + 1) * pageData.size, pageData.totalElements)} of ${pageData.totalElements} entries`);
    let html = '';
    html += `<li class="page-item ${pageData.first ? 'disabled' : ''}"><a class="page-link" href="#" onclick="event.preventDefault(); ${!pageData.first ? `loadApplications(${currentAppPage - 1}, '${currentAppSearch}')` : ''}">Prev</a></li>`;
    html += `<li class="page-item ${pageData.last ? 'disabled' : ''}"><a class="page-link" href="#" onclick="event.preventDefault(); ${!pageData.last ? `loadApplications(${currentAppPage + 1}, '${currentAppSearch}')` : ''}">Next</a></li>`;
    $('#appPagination').html(html);
}

$(document).ready(function() {
    $('#appSearch').on('input', function() {
        const val = $(this).val();
        clearTimeout(appSearchTimeout);
        appSearchTimeout = setTimeout(() => { loadApplications(0, val); }, 400);
    });
});

// ─────────────────────────────────────────────
// STATE MACHINE ACTIONS
// ─────────────────────────────────────────────
function submitApp(id) {
    if(!confirm("Are you sure you want to submit this application?")) return;
    $.post(`/api/loan-applications/${id}/submit`, function() {
        loadApplications(currentAppPage, currentAppSearch);
    }).fail(function(xhr) { alert(xhr.responseJSON?.message || 'Error'); });
}

function reviewApp(id) {
    if(!confirm("Start reviewing this application?")) return;
    $.post(`/api/loan-applications/${id}/review`, function() {
        loadApplications(currentAppPage, currentAppSearch);
    }).fail(function(xhr) { alert(xhr.responseJSON?.message || 'Error'); });
}

function disburseApp(id) {
    if(!confirm("Disburse this loan? This will generate the repayment schedule.")) return;
    $.post(`/api/loans/${id}/disburse`, function() {
        alert("Loan disbursed successfully! Redirecting to Active Loans...");
        loadPage('loans'); // Automatically switch to the Loans tab
    }).fail(function(xhr) { alert(xhr.responseJSON?.message || 'Error'); });
}

function openRecommendModal(id) {
    $('#actionAppId').val(id);
    $('#actionEndpoint').val('recommend');
    $('#actionModalTitle').text('Recommend Application');
    $('#decisionContainer').show();
    $('#actionDecision').html(`
        <option value="RECOMMEND_APPROVE">Recommend Approve</option>
        <option value="RECOMMEND_REJECT">Recommend Reject</option>
    `);
    $('#actionRemarks').val('');
    $('#actionModal').modal('show');
}

function openDecideModal(id, decision) {
    $('#actionAppId').val(id);
    $('#actionEndpoint').val('decide');
    $('#actionModalTitle').text(decision === 'APPROVED' ? 'Approve Application' : 'Reject Application');
    $('#decisionContainer').hide();
    $('#actionRemarks').val('');
    $('#actionConfirmBtn').data('decision', decision);
    $('#actionModal').modal('show');
}

function executeAction() {
    const id = $('#actionAppId').val();
    const endpoint = $('#actionEndpoint').val();
    const remarks = $('#actionRemarks').val();
    
    let decision = '';
    if (endpoint === 'recommend') {
        decision = $('#actionDecision').val(); 
    } else if (endpoint === 'decide') {
        decision = $('#actionConfirmBtn').data('decision'); 
    }

    if (!remarks) { alert("Remarks are required"); return; }

    let url = '';
    let payload = {};

    if (endpoint === 'recommend') {
        url = `/api/loan-applications/${id}/recommend`;
        payload = { decision: decision, officerRemarks: remarks };
    } else if (endpoint === 'decide') {
        url = `/api/loan-applications/${id}/decision`;
        payload = { decision: decision, adminRemarks: remarks };
    }

    $('#actionConfirmBtn').prop('disabled', true).text('Processing...');

    $.ajax({
        url: url,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function() {
            $('#actionModal').modal('hide');
            $('#actionConfirmBtn').prop('disabled', false).text('Confirm');
            loadApplications(currentAppPage, currentAppSearch);
        },
        error: function(xhr) {
            $('#actionConfirmBtn').prop('disabled', false).text('Confirm');
            alert(xhr.responseJSON ? xhr.responseJSON.message : 'Error processing action');
        }
    });
}

// ─────────────────────────────────────────────
// CREATE APPLICATION MODAL & EMI CALCULATOR
// ─────────────────────────────────────────────
function prepareNewApp() {
    $('#appForm')[0].reset();
    $('#appEmiPreview').val('LKR 0.00');
    
    // Fetch Customers
    $.get('/api/customers?size=100&status=ACTIVE', function(res) {
        let options = '<option value="">Select a customer...</option>';
        res.data.content.forEach(c => {
            options += `<option value="${c.id}">${c.customerCode} - ${c.fullName} (${c.nicNumber})</option>`;
        });
        $('#appCustomer').html(options);
    });

    // Fetch Loan Types
    $.get('/api/loan-types?size=100&status=ACTIVE', function(res) {
        let options = '<option value="">Select a loan type...</option>';
        res.data.content.forEach(lt => {
            options += `<option value="${lt.id}" data-rate="${lt.annualInterestRate}" data-min="${lt.minimumAmount}" data-max="${lt.maximumAmount}" data-min-ten="${lt.minimumTenureMonths}" data-max-ten="${lt.maximumTenureMonths}">${lt.loanTypeName} (${lt.annualInterestRate}%)</option>`;
        });
        $('#appLoanType').html(options);
    });
}

function updateLiveEmi() {
    const amount = parseFloat($('#appAmount').val());
    const tenure = parseInt($('#appTenure').val());
    const selectedType = $('#appLoanType option:selected');
    const rate = parseFloat(selectedType.data('rate'));

    if (!amount || !tenure || isNaN(rate)) {
        $('#appEmiPreview').val('LKR 0.00');
        return;
    }
    
    const min = parseFloat(selectedType.data('min'));
    const max = parseFloat(selectedType.data('max'));
    if (amount < min || amount > max) {
        $('#appEmiPreview').val('Amount out of range');
        return;
    }

    $.get(`/api/loans/calculate-emi?principal=${amount}&annualRate=${rate}&tenureMonths=${tenure}`)
        .done(function(res) {
            $('#appEmiPreview').val(formatCurrency(res.data.monthlyEmi));
        })
        .fail(function() {
            $('#appEmiPreview').val('Error calculating');
        });
}

function saveApplication() {
    const payload = {
        customerId: $('#appCustomer').val(),
        loanTypeId: $('#appLoanType').val(),
        requestedAmount: $('#appAmount').val(),
        requestedTenureMonths: $('#appTenure').val(),
        purpose: $('#appPurpose').val()
    };

    $('#saveAppBtn').prop('disabled', true).text('Submitting...');

    $.ajax({
        url: '/api/loan-applications',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function() {
            $('#appModal').modal('hide');
            $('#saveAppBtn').prop('disabled', false).text('Submit Application');
            loadApplications(0, ''); 
        },
        error: function(xhr) {
            $('#saveAppBtn').prop('disabled', false).text('Submit Application');
            alert(xhr.responseJSON ? xhr.responseJSON.message : 'Error creating application');
        }
    });
}