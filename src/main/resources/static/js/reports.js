// reports.js
// Reports page: portfolio summary, interactive chart, daily collection summary,
// and PDF export.

'use strict';

const REPORT_COLSPAN = 3;
let reportsChartInstance = null;

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

function formatLocalDate(d) {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/* ──────────────────────────────────────────────────────────────────────
 * Entry point
 * ──────────────────────────────────────────────────────────────────── */

function loadReports() {
    $('#rep-disbursed, #rep-interest, #rep-outstanding').text('—');

    $.get('/api/reports/portfolio')
        .done(function (res) {
            const data = (res && res.data) || {};
            $('#rep-disbursed').text(formatCurrency(data.totalDisbursedPrincipal));
            $('#rep-interest').text(formatCurrency(data.totalInterestExpected));
            $('#rep-outstanding').text(formatCurrency(data.totalOutstanding));
            
            // Render the chart with the fetched data
            renderReportsChart(data);
        })
        .fail(function () {
            $('#rep-disbursed, #rep-interest, #rep-outstanding').text('—');
            $('#rep-disbursed, #rep-interest, #rep-outstanding').each(function () {
                const $lbl = $(this).closest('.stat-card').find('.stat-label');
                if (!/unavailable/i.test($lbl.text())) {
                    $lbl.text($lbl.text() + ' (unavailable)');
                }
            });
        });

    // Default date range
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
 * Chart Rendering
 * ──────────────────────────────────────────────────────────────────── */

function renderReportsChart(data) {
    const canvas = document.getElementById('reportsChart');
    if (!canvas) return;

    if (reportsChartInstance) {
        reportsChartInstance.destroy();
    }

    const ctx = canvas.getContext('2d');
    reportsChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Disbursed Principal', 'Expected Interest', 'Outstanding Balance'],
            datasets: [{
                label: 'Amount (LKR)',
                data: [
                    data.totalDisbursedPrincipal || 0,
                    data.totalInterestExpected || 0,
                    data.totalOutstanding || 0
                ],
                backgroundColor: [
                    '#2563EB', // Blue
                    '#16A34A', // Green
                    '#DC2626'  // Red
                ],
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: false, // Crucial: disables animation so PDF captures immediately
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { borderDash: [4, 4], color: '#E2E8F0' },
                    ticks: {
                        color: '#000000',
                        callback: function(value) {
                            return 'LKR ' + value.toLocaleString();
                        }
                    }
                },
                x: { 
                    grid: { display: false },
                    ticks: { color: '#000000' }
                }
            }
        }
    });
}

/* ──────────────────────────────────────────────────────────────────────
 * Collection Report & PDF Export
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

function exportReportsPDF() {
    const element = document.getElementById('view-reports');
    const canvas = document.getElementById('reportsChart');
    let imgPlaceholder = null;

    // 1. SWAP CANVAS FOR IMAGE: html2canvas often misses canvas elements.
    // We convert the chart to an image so it's guaranteed to appear in the PDF.
    if (canvas) {
        try {
            const dataUrl = canvas.toDataURL('image/png');
            imgPlaceholder = document.createElement('img');
            imgPlaceholder.src = dataUrl;
            imgPlaceholder.style.width = '100%';
            imgPlaceholder.style.maxHeight = '300px';
            imgPlaceholder.style.objectFit = 'contain';
            canvas.parentNode.replaceChild(imgPlaceholder, canvas);
        } catch (e) {
            console.warn('Could not convert canvas to image', e);
        }
    }

    // 2. ADD HEADER
    const header = document.createElement('div');
    header.id = 'pdf-temp-header';
    header.innerHTML = `
        <div style="text-align: center; margin-bottom: 30px; border-bottom: 2px solid #E2E8F0; padding-bottom: 15px;">
            <h2 style="color: #000000; margin: 0; font-family: 'Inter', sans-serif;">Loan Management System</h2>
            <h4 style="color: #333333; margin: 5px 0 0 0; font-family: 'Inter', sans-serif;">Financial Portfolio Report</h4>
            <p style="color: #666666; margin: 10px 0 0 0; font-size: 12px;">Generated on: ${new Date().toLocaleString()}</p>
        </div>
    `;
    element.insertBefore(header, element.firstChild);

    // 3. APPLY PDF STYLES
    // Remove 'animate-in' (which sets opacity to 0 initially) and add 'pdf-exporting' (forces black text/white bg)
    element.classList.remove('animate-in');
    element.classList.add('pdf-exporting');
    element.style.opacity = '1'; 

    // 4. GENERATE PDF
    const opt = {
        margin:       0.5,
        filename:     `LMS_Financial_Report_${new Date().toISOString().split('T')[0]}.pdf`,
        image:        { type: 'jpeg', quality: 1 },
        html2canvas:  { 
            scale: 2, 
            useCORS: true, 
            backgroundColor: '#ffffff',
            scrollY: 0,
            scrollX: 0
        },
        jsPDF:        { unit: 'in', format: 'a4', orientation: 'portrait' }
    };

    html2pdf().set(opt).from(element).save().then(() => {
        // 5. CLEANUP
        // Remove the header, remove the PDF styles, and restore the chart canvas
        element.removeChild(header);
        element.classList.remove('pdf-exporting');
        element.classList.add('animate-in');
        element.style.opacity = '';

        if (imgPlaceholder && imgPlaceholder.parentNode) {
            imgPlaceholder.parentNode.replaceChild(canvas, imgPlaceholder);
        }
    });
}