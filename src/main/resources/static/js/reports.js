// reports.js
// Reports page: portfolio summary + daily collection summary with date range.
// Depends on: api.js (jQuery setup, CSRF, formatCurrency).

'use strict';

const REPORT_COLSPAN = 3;

/* ──────────────────────────────────────────────────────────────────────
 * Helpers
 * ──────────────────────────────────────────────────────────────────── */

function escapeReportHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * Format a Date as YYYY-MM-DD using LOCAL calendar fields.
 * Do NOT use toISOString() — that returns the UTC date and, in timezones
 * east of UTC (e.g. Asia/Colombo, UTC+5:30), silently shifts the date
 * back by one day at local midnight, corrupting default report ranges.
 */
function formatLocalDate(d) {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/* ──────────────────────────────────────────────────────────────────────
 * Entry point — load portfolio + collection report
 * ──────────────────────────────────────────────────────────────────── */

function loadReports() {
    // Portfolio summary — show loading placeholder so stale values do not
    // linger from a previous visit.
    $('#rep-disbursed, #rep-interest, #rep-outstanding').text('—');

    $.get('/api/reports/portfolio')
        .done(function (res) {
            const data = (res && res.data) || {};
            $('#rep-disbursed').text(formatCurrency(data.totalDisbursedPrincipal));
            $('#rep-interest').text(formatCurrency(data.totalInterestExpected));
            $('#rep-outstanding').text(formatCurrency(data.totalOutstanding));
        })
        .fail(function () {
            $('#rep-disbursed, #rep-interest, #rep-outstanding').text('—');
            $('#rep-disbursed, #rep-interest, #rep-outstanding').each(function () {
                // Append an "unavailable" note only once.
                const $lbl = $(this).closest('.stat-card').find('.stat-label');
                if (!/unavailable/i.test($lbl.text())) {
                    $lbl.text($lbl.text() + ' (unavailable)');
                }
            });
        });

    // Default date range: current month → today (local calendar).
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

    if (!$('#repStartDate').val()) {
        $('#repStartDate').val(formatLocalDate(firstDay));
    }
    if (!$('#repEndDate').val()) {
        $('#repEndDate').val(formatLocalDate(today));
    }

    loadCollectionReport();
}

/* ──────────────────────────────────────────────────────────────────────
 * Collection report
 * ──────────────────────────────────────────────────────────────────── */

function loadCollectionReport() {
    const start = $('#repStartDate').val();
    const end = $('#repEndDate').val();

    if (!start) { alert('Please select a start date.'); $('#repStartDate').focus(); return; }
    if (!end)   { alert('Please select an end date.');   $('#repEndDate').focus();   return; }

    if (start > end) {
        alert('Start date must be on or before end date.');
        return;
    }

    // Visual constraints in the date pickers (harmless if browser ignores them)
    $('#repStartDate').attr('max', end);
    $('#repEndDate').attr('min', start);

    $('#reportTableBody').html(
        `<tr><td colspan="${REPORT_COLSPAN}" class="text-center py-3">
            <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
        </td></tr>`
    );

    $.get('/api/reports/collections', { startDate: start, endDate: end })
        .done(function (res) {
            const rows = (res && res.data) || [];
            let html = '';

            if (!rows.length) {
                html = `<tr><td colspan="${REPORT_COLSPAN}" class="border-0">
                    <div class="empty-state">
                        <i class="bi bi-calendar-x"></i>
                        No collections found for this period
                    </div>
                </td></tr>`;
            } else {
                rows.forEach(r => {
                    const count = (r.transactionCount == null) ? 0 : r.transactionCount;
                    html += `
                        <tr>
                            <td class="ps-4 fw-semibold">${escapeReportHtml(r.date)}</td>
                            <td>${escapeReportHtml(count)} payments</td>
                            <td class="fw-semibold text-success amount">${formatCurrency(r.totalCollected)}</td>
                        </tr>
                    `;
                });
            }
            $('#reportTableBody').html(html);
        })
        .fail(function (xhr) {
            const msg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                ? ' — ' + xhr.responseJSON.message
                : '';
            $('#reportTableBody').html(
                `<tr><td colspan="${REPORT_COLSPAN}" class="text-center py-5 text-danger">
                    Failed to load collections${escapeReportHtml(msg)}
                </td></tr>`
            );
        });
}