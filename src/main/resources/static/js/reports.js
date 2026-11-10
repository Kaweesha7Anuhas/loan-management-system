function loadReports() {
    // 1. Load Portfolio Report
    $.get('/api/reports/portfolio', function(res) {
        const data = res.data;
        $('#rep-disbursed').text(formatCurrency(data.totalDisbursedPrincipal));
        $('#rep-interest').text(formatCurrency(data.totalInterestExpected));
        $('#rep-outstanding').text(formatCurrency(data.totalOutstanding));
    });

    // 2. Set Default Dates for Collection Report (Current Month)
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    
    // Format YYYY-MM-DD
    const formatDate = (d) => d.toISOString().split('T')[0];
    
    if (!$('#repStartDate').val()) {
        $('#repStartDate').val(formatDate(firstDay));
        $('#repEndDate').val(formatDate(today));
    }
    
    loadCollectionReport();
}

function loadCollectionReport() {
    const start = $('#repStartDate').val();
    const end = $('#repEndDate').val();
    
    if (!start || !end) { alert("Please select both dates"); return; }

    $('#reportTableBody').html('<tr><td colspan="3" class="text-center py-3"><div class="spinner-border spinner-border-sm text-primary"></div></td></tr>');

    $.get(`/api/reports/collections?startDate=${start}&endDate=${end}`, function(res) {
        let html = '';
        if (res.data.length === 0) {
            html = '<tr><td colspan="3" class="text-center py-4 text-muted">No collections found for this period</td></tr>';
        } else {
            res.data.forEach(r => {
                html += `
                    <tr>
                        <td class="ps-4 fw-bold">${r.date}</td>
                        <td>${r.transactionCount} payments</td>
                        <td class="fw-bold text-success">${formatCurrency(r.totalCollected)}</td>
                    </tr>
                `;
            });
        }
        $('#reportTableBody').html(html);
    });
}