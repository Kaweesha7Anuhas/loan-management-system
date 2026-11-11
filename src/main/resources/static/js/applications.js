// applications.js
// Loan Application view: listing, workflow actions, EMI calculator, create modal.
// Depends on: api.js (formatCurrency, jQuery setup), dashboard.js (loadPage),
//             Bootstrap 5 modals (loaded via bundle in index.html).

'use strict';

/* ──────────────────────────────────────────────────────────────────────────
 * State
 * ──────────────────────────────────────────────────────────────────────── */

var currentAppPage = 0;
var currentAppSearch = '';
var appSearchTimeout = null;
var emiCalcTimeout = null;
var emiRequestSeq = 0;

/* ──────────────────────────────────────────────────────────────────────────
 * Small helpers
 * ──────────────────────────────────────────────────────────────────────── */

function escapeAppHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// Bootstrap 5 removed the jQuery .modal() bridge — use the JS API.
function showBsModal(id) {
    var el = document.getElementById(id);
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).show();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) { // legacy fallback
        $(el).modal('show');
    }
}

function hideBsModal(id) {
    var el = document.getElementById(id);
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).hide();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) {
        $(el).modal('hide');
    }
}

function renderAppStatusBadge(status) {
    var map = {
        'APPROVED':     'status-success',
        'REJECTED':     'status-danger',
        'SUBMITTED':    'status-warning',
        'UNDER_REVIEW': 'status-warning',
        'RECOMMENDED':  'status-info',
        'DRAFT':        'status-neutral'
    };
    var cls = map[status] || 'status-neutral';
    return '<span class="status-badge ' + cls + '">' + escapeAppHtml(status) + '</span>';
}

/* ──────────────────────────────────────────────────────────────────────────
 * Table rendering
 * ──────────────────────────────────────────────────────────────────────── */

function loadApplications(page, search) {
    currentAppPage = page;
    currentAppSearch = search == null ? '' : search;

    $('#appTableBody').html(
        '<tr><td colspan="5" class="text-center py-4">' +
            '<div class="spinner-border text-primary" role="status"></div>' +
        '</td></tr>'
    );

    $.get('/api/loan-applications', {
        page: page,
        size: 10,
        search: currentAppSearch,
        sortBy: 'createdAt',
        sortDir: 'desc'
    })
        .done(function (res) {
            var data = res.data || {};
            var rows = '';

            if (!data.content || !data.content.length) {
                rows =
                    '<tr><td colspan="5" class="border-0">' +
                        '<div class="empty-state">' +
                            '<i class="bi bi-file-earmark-text"></i>' +
                            'No applications found' +
                        '</div>' +
                    '</td></tr>';
            } else {
                data.content.forEach(function (a) {
                    rows += buildApplicationRow(a);
                });
            }

            $('#appTableBody').html(rows);
            renderAppPagination(data);
        })
        .fail(function () {
            $('#appTableBody').html(
                '<tr><td colspan="5" class="text-center py-5 text-danger">' +
                    'Failed to load applications' +
                '</td></tr>'
            );
            renderAppPagination({
                page: 0, size: 10, totalElements: 0, first: true, last: true
            });
        });
}

function buildApplicationRow(a) {
    return (
        '<tr>' +
            '<td class="ps-4 fw-bold text-primary">' +
                escapeAppHtml(a.applicationNumber) +
            '</td>' +
            '<td>' +
                '<div class="fw-semibold">' + escapeAppHtml(a.customerFullName) + '</div>' +
                '<div class="small text-muted">' + escapeAppHtml(a.loanTypeName) + '</div>' +
            '</td>' +
            '<td class="amount">' + formatCurrency(a.requestedAmount) + '</td>' +
            '<td>' + renderAppStatusBadge(a.applicationStatus) + '</td>' +
            '<td class="text-end pe-4">' + buildAppActionButtons(a) + '</td>' +
        '</tr>'
    );
}

function buildAppActionButtons(a) {
    var role = window.currentUserRole;
    var status = a.applicationStatus;

    if (status === 'DRAFT') {
        return '<button class="btn btn-sm btn-primary" onclick="submitApp(' + a.id + ')">Submit</button>';
    }
    if (status === 'SUBMITTED') {
        return '<button class="btn btn-sm btn-warning text-dark" onclick="reviewApp(' + a.id + ')">Start Review</button>';
    }
    if (status === 'UNDER_REVIEW') {
        return '<button class="btn btn-sm btn-info text-dark" onclick="openRecommendModal(' + a.id + ')">Recommend</button>';
    }
    if (status === 'RECOMMENDED') {
        if (role === 'ADMIN') {
            return (
                '<button class="btn btn-sm btn-success" onclick="openDecideModal(' + a.id + ', \'APPROVED\')">Approve</button>' +
                '<button class="btn btn-sm btn-danger ms-1" onclick="openDecideModal(' + a.id + ', \'REJECTED\')">Reject</button>'
            );
        }
        return '<span class="text-muted small">Awaiting Admin</span>';
    }
    if (status === 'APPROVED') {
        if (role === 'ADMIN') {
            if (a.disbursed) {
                return '<button class="btn btn-sm btn-outline-primary" onclick="loadPage(\'loans\')">View Loan</button>';
            }
            return '<button class="btn btn-sm btn-dark" onclick="disburseApp(' + a.id + ')">Disburse</button>';
        }
        return '<span class="text-muted small">Approved</span>';
    }
    return '<span class="text-muted small">No actions</span>';
}

function renderAppPagination(pageData) {
    var totalElements = pageData.totalElements || 0;
    var page = pageData.page || 0;
    var size = pageData.size || 10;
    var totalPages = pageData.totalPages || Math.max(1, Math.ceil(totalElements / size));

    var start = totalElements === 0 ? 0 : page * size + 1;
    var end = Math.min((page + 1) * size, totalElements);

    $('#appPaginationInfo').text(
        'Showing ' + start + ' to ' + end + ' of ' + totalElements + ' entries'
    );

    if (totalPages <= 1) {
        $('#appPagination').empty();
        return;
    }

    var html = '';

    // Prev
    html += '<li class="page-item ' + (pageData.first ? 'disabled' : '') + '">' +
                '<a class="page-link" href="#" data-page="' + (page - 1) + '">Prev</a>' +
            '</li>';

    // Page-number window with ellipses
    var win = 2;
    var from = Math.max(0, page - win);
    var to = Math.min(totalPages - 1, page + win);

    if (from > 0) {
        html += '<li class="page-item"><a class="page-link" href="#" data-page="0">1</a></li>';
        if (from > 1) {
            html += '<li class="page-item disabled"><span class="page-link">…</span></li>';
        }
    }
    for (var i = from; i <= to; i++) {
        html += '<li class="page-item ' + (i === page ? 'active' : '') + '">' +
                    '<a class="page-link" href="#" data-page="' + i + '">' + (i + 1) + '</a>' +
                '</li>';
    }
    if (to < totalPages - 1) {
        if (to < totalPages - 2) {
            html += '<li class="page-item disabled"><span class="page-link">…</span></li>';
        }
        html += '<li class="page-item"><a class="page-link" href="#" data-page="' + (totalPages - 1) + '">' + totalPages + '</a></li>';
    }

    // Next
    html += '<li class="page-item ' + (pageData.last ? 'disabled' : '') + '">' +
                '<a class="page-link" href="#" data-page="' + (page + 1) + '">Next</a>' +
            '</li>';

    $('#appPagination').html(html);
}

/* ──────────────────────────────────────────────────────────────────────────
 * Search + pagination event wiring
 * ──────────────────────────────────────────────────────────────────────── */

$(function () {
    $('#appSearch').on('input', function () {
        var value = $(this).val();
        clearTimeout(appSearchTimeout);
        appSearchTimeout = setTimeout(function () {
            loadApplications(0, value);
        }, 400);
    });
});

$(document).on('click', '#appPagination a[data-page]', function (e) {
    e.preventDefault();
    if ($(this).closest('.page-item').hasClass('disabled')) return;
    var target = parseInt($(this).attr('data-page'), 10);
    if (!isNaN(target) && target >= 0) {
        loadApplications(target, currentAppSearch);
    }
});

/* ──────────────────────────────────────────────────────────────────────────
 * Workflow actions
 * ──────────────────────────────────────────────────────────────────────── */

function submitApp(id) {
    if (!confirm('Are you sure you want to submit this application?')) return;
    $.post('/api/loan-applications/' + id + '/submit')
        .done(function () {
            loadApplications(currentAppPage, currentAppSearch);
        })
        .fail(function (xhr) {
            alert((xhr.responseJSON && xhr.responseJSON.message) || 'Error');
        });
}

function reviewApp(id) {
    if (!confirm('Start reviewing this application?')) return;
    $.post('/api/loan-applications/' + id + '/review')
        .done(function () {
            loadApplications(currentAppPage, currentAppSearch);
        })
        .fail(function (xhr) {
            alert((xhr.responseJSON && xhr.responseJSON.message) || 'Error');
        });
}

function disburseApp(id) {
    if (!confirm('Disburse this loan? This will generate the repayment schedule.')) return;
    $.post('/api/loans/' + id + '/disburse')
        .done(function () {
            alert('Loan disbursed successfully! Redirecting to Active Loans...');
            loadPage('loans');
        })
        .fail(function (xhr) {
            alert((xhr.responseJSON && xhr.responseJSON.message) || 'Error');
        });
}

/* ──────────────────────────────────────────────────────────────────────────
 * Action modal (Recommend / Approve / Reject)
 * ──────────────────────────────────────────────────────────────────────── */

function openRecommendModal(id) {
    $('#actionAppId').val(id);
    $('#actionEndpoint').val('recommend');
    $('#actionModalTitle').text('Recommend Application');
    $('#decisionContainer').show();
    $('#actionDecision').html(
        '<option value="RECOMMEND_APPROVE">Recommend Approve</option>' +
        '<option value="RECOMMEND_REJECT">Recommend Reject</option>'
    );
    $('#actionRemarks').val('');
    $('#actionConfirmBtn')
        .prop('disabled', false)
        .text('Confirm')
        .removeData('decision');
    showBsModal('actionModal');
}

function openDecideModal(id, decision) {
    $('#actionAppId').val(id);
    $('#actionEndpoint').val('decide');
    $('#actionModalTitle').text(
        decision === 'APPROVED' ? 'Approve Application' : 'Reject Application'
    );
    $('#decisionContainer').hide();
    $('#actionRemarks').val('');
    $('#actionConfirmBtn')
        .prop('disabled', false)
        .text('Confirm')
        .data('decision', decision);
    showBsModal('actionModal');
}

function executeAction() {
    var id = $('#actionAppId').val();
    var endpoint = $('#actionEndpoint').val();
    var remarks = ($('#actionRemarks').val() || '').trim();

    var decision = '';
    if (endpoint === 'recommend') {
        decision = $('#actionDecision').val();
    } else if (endpoint === 'decide') {
        decision = $('#actionConfirmBtn').data('decision');
    }

    if (!remarks) {
        alert('Remarks are required');
        return;
    }

    var url, payload;
    if (endpoint === 'recommend') {
        url = '/api/loan-applications/' + id + '/recommend';
        payload = { decision: decision, officerRemarks: remarks };
    } else if (endpoint === 'decide') {
        url = '/api/loan-applications/' + id + '/decision';
        payload = { decision: decision, adminRemarks: remarks };
    } else {
        return;
    }

    var $btn = $('#actionConfirmBtn').prop('disabled', true).text('Processing...');

    $.ajax({
        url: url,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload)
    })
        .done(function () {
            hideBsModal('actionModal');
            $btn.prop('disabled', false).text('Confirm');
            loadApplications(currentAppPage, currentAppSearch);
        })
        .fail(function (xhr) {
            $btn.prop('disabled', false).text('Confirm');
            alert((xhr.responseJSON && xhr.responseJSON.message) || 'Error processing action');
        });
}

/* ──────────────────────────────────────────────────────────────────────────
 * New Application modal + live EMI calculator
 * ──────────────────────────────────────────────────────────────────────── */

function prepareNewApp() {
    var form = document.getElementById('appForm');
    if (form) form.reset();

    $('#appEmiPreview').val('LKR 0.00');

    // Customers
    $.get('/api/customers', { size: 100, status: 'ACTIVE' })
        .done(function (res) {
            var options = '<option value="">Select a customer...</option>';
            (res.data && res.data.content || []).forEach(function (c) {
                options += '<option value="' + c.id + '">' +
                    escapeAppHtml(c.customerCode) + ' - ' +
                    escapeAppHtml(c.fullName) + ' (' +
                    escapeAppHtml(c.nicNumber) + ')' +
                '</option>';
            });
            $('#appCustomer').html(options);
        });

    // Loan types
    $.get('/api/loan-types', { size: 100, status: 'ACTIVE' })
        .done(function (res) {
            var options = '<option value="">Select a loan type...</option>';
            (res.data && res.data.content || []).forEach(function (lt) {
                options += '<option value="' + lt.id + '"' +
                    ' data-rate="' + lt.annualInterestRate + '"' +
                    ' data-min="' + lt.minimumAmount + '"' +
                    ' data-max="' + lt.maximumAmount + '"' +
                    ' data-min-ten="' + lt.minimumTenureMonths + '"' +
                    ' data-max-ten="' + lt.maximumTenureMonths + '">' +
                    escapeAppHtml(lt.loanTypeName) + ' (' + lt.annualInterestRate + '%)' +
                '</option>';
            });
            $('#appLoanType').html(options);
        });
}

// Debounced entry-point — called from HTML inline oninput / onchange.
function updateLiveEmi() {
    clearTimeout(emiCalcTimeout);
    emiCalcTimeout = setTimeout(computeEmi, 300);
}

function computeEmi() {
    var amount = parseFloat($('#appAmount').val());
    var tenure = parseInt($('#appTenure').val(), 10);
    var $selected = $('#appLoanType option:selected');
    var rate = parseFloat($selected.data('rate'));

    if (!amount || !tenure || isNaN(rate)) {
        $('#appEmiPreview').val('LKR 0.00');
        return;
    }

    var min = parseFloat($selected.data('min'));
    var max = parseFloat($selected.data('max'));
    if (!isNaN(min) && !isNaN(max) && (amount < min || amount > max)) {
        $('#appEmiPreview').val('Amount out of range');
        return;
    }

    var seq = ++emiRequestSeq;

    $.get('/api/loans/calculate-emi', {
        principal: amount,
        annualRate: rate,
        tenureMonths: tenure
    })
        .done(function (res) {
            if (seq !== emiRequestSeq) return;   // stale response, ignore
            $('#appEmiPreview').val(formatCurrency(res.data.monthlyEmi));
        })
        .fail(function () {
            if (seq !== emiRequestSeq) return;
            $('#appEmiPreview').val('Error calculating');
        });
}

function saveApplication() {
    var customerId = $('#appCustomer').val();
    var loanTypeId = $('#appLoanType').val();

    if (!customerId || !loanTypeId) {
        alert('Please select a customer and a loan type.');
        return;
    }

    var payload = {
        customerId: customerId,
        loanTypeId: loanTypeId,
        requestedAmount: $('#appAmount').val(),
        requestedTenureMonths: $('#appTenure').val(),
        purpose: $('#appPurpose').val()
    };

    var $btn = $('#saveAppBtn').prop('disabled', true).text('Submitting...');

    $.ajax({
        url: '/api/loan-applications',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload)
    })
        .done(function () {
            hideBsModal('appModal');
            $btn.prop('disabled', false).text('Submit Application');
            loadApplications(0, '');
        })
        .fail(function (xhr) {
            $btn.prop('disabled', false).text('Submit Application');
            alert((xhr.responseJSON && xhr.responseJSON.message) || 'Error creating application');
        });
}