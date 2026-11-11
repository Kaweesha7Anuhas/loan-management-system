// loan-types.js
// Loan Products management: listing + create.
// (Edit/delete actions are not implemented on this page; the pencil button
//  is intentionally disabled rather than silently doing nothing.)
// Depends on: api.js (jQuery setup, CSRF, formatCurrency), Bootstrap 5 modals.

'use strict';

var LOAN_TYPE_COLSPAN = 7;

/* ──────────────────────────────────────────────────────────────────────
 * Helpers
 * ──────────────────────────────────────────────────────────────────── */

function escapeLoanTypeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function closeLoanTypeModal() {
    var el = document.getElementById('loanTypeModal');
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).hide();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) {
        $(el).modal('hide'); // legacy fallback
    }
}

/* ──────────────────────────────────────────────────────────────────────
 * List
 * ──────────────────────────────────────────────────────────────────── */

function loadLoanTypes() {
    $('#loanTypeTableBody').html(
        `<tr><td colspan="${LOAN_TYPE_COLSPAN}" class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
        </td></tr>`
    );

    $.get('/api/loan-types', { size: 100 })
        .done(function (res) {
            const data = (res && res.data) || null;
            const content = (data && data.content) || null;
            let html = '';

            if (!content) {
                // Malformed / unexpected shape from server.
                html = `<tr><td colspan="${LOAN_TYPE_COLSPAN}" class="text-center py-5 text-danger">
                    Invalid response format from server
                </td></tr>`;
            } else if (!content.length) {
                html = `<tr><td colspan="${LOAN_TYPE_COLSPAN}" class="border-0">
                    <div class="empty-state">
                        <i class="bi bi-tags"></i>
                        No loan types found. Click "New Loan Type" to create one.
                    </div>
                </td></tr>`;
            } else {
                content.forEach(lt => {
                    const statusCls = lt.loanTypeStatus === 'ACTIVE'
                        ? 'status-success'
                        : 'status-danger';

                    const rate = Number(lt.annualInterestRate);
                    const rateText = isFinite(rate) ? rate + '%' : '-';

                    const minAmt = formatCurrency(lt.minimumAmount);
                    const maxAmt = formatCurrency(lt.maximumAmount);

                    const minTen = lt.minimumTenureMonths != null ? lt.minimumTenureMonths : '-';
                    const maxTen = lt.maximumTenureMonths != null ? lt.maximumTenureMonths : '-';

                    html += `
                        <tr>
                            <td class="ps-4 fw-semibold text-primary">${escapeLoanTypeHtml(lt.loanTypeCode)}</td>
                            <td>${escapeLoanTypeHtml(lt.loanTypeName)}</td>
                            <td class="amount">${minAmt} - ${maxAmt}</td>
                            <td class="fw-semibold text-success amount">${rateText}</td>
                            <td class="amount">${minTen} - ${maxTen} months</td>
                            <td>
                                <span class="status-badge ${statusCls}">${escapeLoanTypeHtml(lt.loanTypeStatus)}</span>
                            </td>
                            <td class="text-end pe-4">
                                <button class="btn btn-light text-primary btn-icon"
                                        title="Editing is not available yet"
                                        disabled>
                                    <i class="bi bi-pencil"></i>
                                </button>
                            </td>
                        </tr>
                    `;
                });
            }
            $('#loanTypeTableBody').html(html);
        })
        .fail(function (xhr) {
            const rawMsg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : (xhr && xhr.statusText) || 'Unknown error';
            const msg = escapeLoanTypeHtml(rawMsg);
            $('#loanTypeTableBody').html(
                `<tr><td colspan="${LOAN_TYPE_COLSPAN}" class="text-center py-5 text-danger">
                    Error loading data: ${msg}
                </td></tr>`
            );
        });
}

/* ──────────────────────────────────────────────────────────────────────
 * Form: clear + save
 * ──────────────────────────────────────────────────────────────────── */

function clearLoanTypeForm() {
    const form = document.getElementById('loanTypeForm');
    if (form) form.reset();
}

// Keyboard-friendly: Enter inside the form triggers save (matches modal UX).
$(function () {
    $('#loanTypeForm').on('submit', function (e) {
        e.preventDefault();
        saveLoanType();
    });
});

function saveLoanType() {
    // 1. Native HTML5 constraint validation (required, type="number", etc.)
    const form = document.getElementById('loanTypeForm');
    if (form && typeof form.reportValidity === 'function' && !form.checkValidity()) {
        form.reportValidity();
        return;
    }

    // 2. Client-side sanity checks (cheap and catch common mistakes early).
    const minAmt = Number($('#ltMinAmt').val());
    const maxAmt = Number($('#ltMaxAmt').val());
    const rate   = Number($('#ltRate').val());
    const minTen = Number($('#ltMinTen').val());
    const maxTen = Number($('#ltMaxTen').val());

    if (!(minAmt > 0)) {
        alert('Minimum amount must be greater than 0.');
        return;
    }
    if (!(maxAmt >= minAmt)) {
        alert('Maximum amount must be greater than or equal to the minimum amount.');
        return;
    }
    if (!(rate > 0)) {
        alert('Annual interest rate must be greater than 0.');
        return;
    }
    if (!(minTen >= 1)) {
        alert('Minimum tenure must be at least 1 month.');
        return;
    }
    if (!(maxTen >= minTen)) {
        alert('Maximum tenure must be greater than or equal to the minimum tenure.');
        return;
    }

    // 3. Build the payload — exactly the same keys / order as before.
    const payload = {
        loanTypeCode: $('#ltCode').val(),
        loanTypeName: $('#ltName').val(),
        description: $('#ltDesc').val(),
        minimumAmount: $('#ltMinAmt').val(),
        maximumAmount: $('#ltMaxAmt').val(),
        annualInterestRate: $('#ltRate').val(),
        minimumTenureMonths: $('#ltMinTen').val(),
        maximumTenureMonths: $('#ltMaxTen').val()
    };

    const $btn = $('#saveLtBtn').prop('disabled', true).text('Saving...');

    $.ajax({
        url: '/api/loan-types',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload)
    })
        .done(function () {
            closeLoanTypeModal();
            $btn.prop('disabled', false).text('Save Loan Type');
            loadLoanTypes();
        })
        .fail(function (xhr) {
            $btn.prop('disabled', false).text('Save Loan Type');
            const msg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : 'Error saving loan type';
            alert(msg); // alert() is text-safe; no XSS possible here.
        });
}