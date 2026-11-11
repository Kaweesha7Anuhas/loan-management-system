// loans.js
// Active Loans list, loan details + schedule, and payment recording.
// Depends on: api.js (jQuery setup, CSRF, formatCurrency), Bootstrap 5 modals,
//             dashboard.js (loadPage for the "Back to Loans" button).

'use strict';

let currentLoanPage = 0;
let currentLoanSearch = '';
let loanSearchTimeout = null;
let activeLoanId = null;
let activeLoanOutstanding = 0;   // populated by viewLoan(); used by payment flow

const LOAN_COLSPAN = 6;
const SCHEDULE_COLSPAN = 8;

/* ──────────────────────────────────────────────────────────────────────
 * Helpers
 * ──────────────────────────────────────────────────────────────────── */

function escapeLoanHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function openPaymentModalEl() {
    var el = document.getElementById('paymentModal');
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).show();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) $(el).modal('show');
}

function closePaymentModalEl() {
    var el = document.getElementById('paymentModal');
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).hide();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) $(el).modal('hide');
}

function loanStatusClass(status) {
    if (status === 'ACTIVE')    return 'status-success';
    if (status === 'COMPLETED') return 'status-neutral';
    return 'status-danger';
}

function scheduleStatusClass(status) {
    if (status === 'PAID')           return 'status-success';
    if (status === 'PARTIALLY_PAID') return 'status-warning';
    if (status === 'OVERDUE')        return 'status-danger';
    return 'status-neutral';
}

/* ──────────────────────────────────────────────────────────────────────
 * Loans list
 * ──────────────────────────────────────────────────────────────────── */

function loadLoans(page, search) {
    currentLoanPage = page;
    currentLoanSearch = search == null ? '' : search;

    $('#loanTableBody').html(
        `<tr><td colspan="${LOAN_COLSPAN}" class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
        </td></tr>`
    );

    $.get('/api/loans', {
        page: page,
        size: 10,
        search: currentLoanSearch,
        sortBy: 'createdAt',
        sortDir: 'desc'
    })
        .done(function (res) {
            const data = res.data || {};
            const content = data.content || [];
            let html = '';

            if (!content.length) {
                html = `<tr><td colspan="${LOAN_COLSPAN}" class="border-0">
                    <div class="empty-state">
                        <i class="bi bi-cash-stack"></i>
                        No loans found
                    </div>
                </td></tr>`;
            } else {
                content.forEach(l => {
                    html += `
                        <tr>
                            <td class="ps-4 fw-semibold text-primary">${escapeLoanHtml(l.loanAccountNumber)}</td>
                            <td>${escapeLoanHtml(l.customerFullName)}</td>
                            <td class="amount">${formatCurrency(l.principalAmount)}</td>
                            <td class="text-danger fw-semibold amount">${formatCurrency(l.outstandingBalance)}</td>
                            <td>
                                <span class="status-badge ${loanStatusClass(l.loanStatus)}">${escapeLoanHtml(l.loanStatus)}</span>
                            </td>
                            <td class="text-end pe-4">
                                <button class="btn btn-primary btn-sm"
                                        onclick="viewLoan(${Number(l.id)})">View Details</button>
                            </td>
                        </tr>
                    `;
                });
            }
            $('#loanTableBody').html(html);
            renderLoanPagination(data);
        })
        .fail(function () {
            $('#loanTableBody').html(
                `<tr><td colspan="${LOAN_COLSPAN}" class="text-center py-5 text-danger">
                    Failed to load loans
                </td></tr>`
            );
            renderLoanPagination({
                page: 0, size: 10, totalElements: 0, totalPages: 1,
                first: true, last: true, empty: true
            });
        });
}

function renderLoanPagination(pageData) {
    const totalElements = pageData.totalElements || 0;
    const page = pageData.page || 0;
    const size = pageData.size || 10;
    const totalPages = pageData.totalPages || Math.max(1, Math.ceil(totalElements / size));

    const start = totalElements === 0 ? 0 : page * size + 1;
    const end = Math.min((page + 1) * size, totalElements);

    $('#loanPaginationInfo').text(
        `Showing ${start} to ${end} of ${totalElements} entries`
    );

    if (totalPages <= 1) {
        $('#loanPagination').empty();
        return;
    }

    let html = '';

    html += `<li class="page-item ${pageData.first ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${page - 1}">Prev</a>
             </li>`;

    const win = 2;
    const from = Math.max(0, page - win);
    const to = Math.min(totalPages - 1, page + win);

    if (from > 0) {
        html += `<li class="page-item"><a class="page-link" href="#" data-page="0">1</a></li>`;
        if (from > 1) {
            html += `<li class="page-item disabled"><span class="page-link">…</span></li>`;
        }
    }
    for (let i = from; i <= to; i++) {
        html += `<li class="page-item ${i === page ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                 </li>`;
    }
    if (to < totalPages - 1) {
        if (to < totalPages - 2) {
            html += `<li class="page-item disabled"><span class="page-link">…</span></li>`;
        }
        html += `<li class="page-item">
                    <a class="page-link" href="#" data-page="${totalPages - 1}">${totalPages}</a>
                 </li>`;
    }

    html += `<li class="page-item ${pageData.last ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${page + 1}">Next</a>
             </li>`;

    $('#loanPagination').html(html);
}

/* ──────────────────────────────────────────────────────────────────────
 * Event wiring
 * ──────────────────────────────────────────────────────────────────── */

$(function () {
    $('#loanSearch').on('input', function () {
        const val = $(this).val();
        clearTimeout(loanSearchTimeout);
        loanSearchTimeout = setTimeout(() => { loadLoans(0, val); }, 400);
    });
});

// Delegated pagination handler — safe against HTML injection in search text.
$(document).on('click', '#loanPagination a[data-page]', function (e) {
    e.preventDefault();
    const $li = $(this).closest('.page-item');
    if ($li.hasClass('disabled') || $li.hasClass('active')) return;
    const target = parseInt($(this).attr('data-page'), 10);
    if (!isNaN(target) && target >= 0) {
        loadLoans(target, currentLoanSearch);
    }
});

/* ──────────────────────────────────────────────────────────────────────
 * Loan details + schedule
 * ──────────────────────────────────────────────────────────────────── */

function viewLoan(id) {
    activeLoanId = id;
    activeLoanOutstanding = 0;

    $('#view-loans').hide();
    $('#view-loan-details').fadeIn();

    // Detail card — show loading skeleton-ish state on each field
    $('#detail-loan-account, #detail-customer-name, #detail-principal, ' +
      '#detail-rate, #detail-payable, #detail-outstanding')
        .text('…');

    $.get(`/api/loans/${id}`)
        .done(function (res) {
            const l = (res && res.data) || {};

            activeLoanOutstanding = Number(l.outstandingBalance) || 0;

            $('#detail-loan-account').text(l.loanAccountNumber || '-');
            $('#detail-customer-name').text(l.customerFullName || '-');
            $('#detail-principal').text(formatCurrency(l.principalAmount));
            $('#detail-rate').text(
                l.annualInterestRate != null ? (l.annualInterestRate + '%') : '-'
            );
            $('#detail-payable').text(formatCurrency(l.totalPayable));
            $('#detail-outstanding').text(formatCurrency(l.outstandingBalance));

            const $status = $('#detail-loan-status');
            $status
                .removeClass('bg-success bg-secondary bg-danger bg-warning')
                .text(l.loanStatus || '-');
            if (l.loanStatus === 'ACTIVE') {
                $status.addClass('bg-success');
            } else if (l.loanStatus === 'COMPLETED') {
                $status.addClass('bg-secondary');
            } else {
                $status.addClass('bg-danger');
            }

            // Fix: re-show the payment button for ACTIVE loans, hide for others.
            if (l.loanStatus === 'ACTIVE') {
                $('#btnRecordPayment').show();
            } else {
                $('#btnRecordPayment').hide();
            }
        })
        .fail(function (xhr) {
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                || 'Failed to load loan details';
            // Clear stale values so users don't confuse them for the current loan.
            $('#detail-loan-account, #detail-customer-name, #detail-principal, ' +
              '#detail-rate, #detail-payable, #detail-outstanding, #detail-loan-status')
                .text('-');
            $('#btnRecordPayment').hide();
            alert(msg);
        });

    // Schedule
    $('#scheduleTableBody').html(
        `<tr><td colspan="${SCHEDULE_COLSPAN}" class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
        </td></tr>`
    );

    $.get(`/api/loans/${id}/schedule`)
        .done(function (res) {
            const rows = (res && res.data) || [];
            let html = '';

            if (!rows.length) {
                html = `<tr><td colspan="${SCHEDULE_COLSPAN}" class="border-0">
                    <div class="empty-state">
                        <i class="bi bi-calendar-x"></i>
                        No schedule generated yet
                    </div>
                </td></tr>`;
            } else {
                rows.forEach(s => {
                    html += `
                        <tr>
                            <td class="ps-4">${escapeLoanHtml(s.instalmentNumber)}</td>
                            <td>${escapeLoanHtml(s.dueDate)}</td>
                            <td class="fw-semibold amount">${formatCurrency(s.instalmentAmount)}</td>
                            <td class="amount">${formatCurrency(s.principalAmount)}</td>
                            <td class="amount">${formatCurrency(s.interestAmount)}</td>
                            <td class="text-success amount">${formatCurrency(s.paidAmount)}</td>
                            <td class="text-danger amount">${formatCurrency(s.remainingAmount)}</td>
                            <td>
                                <span class="status-badge ${scheduleStatusClass(s.paymentStatus)}">${escapeLoanHtml(s.paymentStatus)}</span>
                            </td>
                        </tr>
                    `;
                });
            }
            $('#scheduleTableBody').html(html);
        })
        .fail(function () {
            $('#scheduleTableBody').html(
                `<tr><td colspan="${SCHEDULE_COLSPAN}" class="text-center py-5 text-danger">
                    Failed to load repayment schedule
                </td></tr>`
            );
        });
}

/* ──────────────────────────────────────────────────────────────────────
 * Payment modal
 * ──────────────────────────────────────────────────────────────────── */

function openPaymentModal() {
    $('#payAmount').val('').attr('max', activeLoanOutstanding);
    $('#payRef').val('');
    $('#payMethod').val('CASH');
    $('#payOutstandingHint').text('Max: ' + formatCurrency(activeLoanOutstanding));
    openPaymentModalEl();
}

function submitPayment() {
    if (!activeLoanId) {
        alert('No loan selected.');
        return;
    }

    const rawAmount = $('#payAmount').val();
    const amount = parseFloat(rawAmount);

    if (!isFinite(amount) || amount <= 0) {
        alert('Enter a valid amount');
        return;
    }

    // Small float tolerance (0.005) so "exact" over-payments due to rounding
    // are accepted; anything beyond that is rejected client-side.
    if (activeLoanOutstanding > 0 && amount > activeLoanOutstanding + 0.005) {
        alert('Amount exceeds outstanding balance (' +
              formatCurrency(activeLoanOutstanding) + ').');
        return;
    }

    const payload = {
        loanId: activeLoanId,
        paymentAmount: rawAmount,
        paymentMethod: $('#payMethod').val(),
        transactionReference: $('#payRef').val() || null
    };

    const $btn = $('#payBtn').prop('disabled', true).text('Processing...');

    $.ajax({
        url: '/api/repayments',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload)
    })
        .done(function (res) {
            closePaymentModalEl();
            $btn.prop('disabled', false).text('Record Payment');

            const receipt = (res && res.data && res.data.receiptNumber)
                ? ' Receipt: ' + res.data.receiptNumber
                : '';
            alert('Payment of ' + formatCurrency(amount) + ' recorded.' + receipt);

            // Reload detail card to reflect new balances / status.
            viewLoan(activeLoanId);
        })
        .fail(function (xhr) {
            $btn.prop('disabled', false).text('Record Payment');
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                || 'Error recording payment';
            alert(msg);
        });
}