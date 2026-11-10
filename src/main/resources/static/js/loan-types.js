function loadLoanTypes() {
    $('#loanTypeTableBody').html('<tr><td colspan="7" class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></td></tr>');

    $.get('/api/loan-types?size=100')
        .done(function(res) {
            let html = '';
            // Check if data exists
            if (!res.data || !res.data.content) {
                html = '<tr><td colspan="7" class="text-center py-5 text-danger">Invalid response format from server</td></tr>';
            } else if (res.data.empty) {
                html = '<tr><td colspan="7" class="text-center py-5 text-muted">No loan types found. Click "New Loan Type" to create one.</td></tr>';
            } else {
                res.data.content.forEach(lt => {
                    const badge = lt.loanTypeStatus === 'ACTIVE' ? 'bg-success' : 'bg-danger';
                    html += `
                        <tr>
                            <td class="ps-4 fw-bold text-primary">${lt.loanTypeCode}</td>
                            <td>${lt.loanTypeName}</td>
                            <td>${formatCurrency(lt.minimumAmount)} - ${formatCurrency(lt.maximumAmount)}</td>
                            <td class="fw-bold text-success">${lt.annualInterestRate}%</td>
                            <td>${lt.minimumTenureMonths} - ${lt.maximumTenureMonths} months</td>
                            <td><span class="badge ${badge} rounded-pill px-3">${lt.loanTypeStatus}</span></td>
                            <td class="text-end pe-4"><button class="btn btn-sm btn-light text-primary"><i class="bi bi-pencil"></i></button></td>
                        </tr>
                    `;
                });
            }
            $('#loanTypeTableBody').html(html);
        })
        .fail(function(xhr) {
            // If the request fails, show the error message
            const msg = xhr.responseJSON ? xhr.responseJSON.message : xhr.statusText;
            $('#loanTypeTableBody').html(`<tr><td colspan="7" class="text-center py-5 text-danger">Error loading data: ${msg}</td></tr>`);
        });
}

function clearLoanTypeForm() {
    $('#loanTypeForm')[0].reset();
}

function saveLoanType() {
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

    $('#saveLtBtn').prop('disabled', true).text('Saving...');

    $.ajax({
        url: '/api/loan-types',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function() {
            $('#loanTypeModal').modal('hide');
            $('#saveLtBtn').prop('disabled', false).text('Save Loan Type');
            loadLoanTypes();
        },
        error: function(xhr) {
            $('#saveLtBtn').prop('disabled', false).text('Save Loan Type');
            alert(xhr.responseJSON ? xhr.responseJSON.message : 'Error saving loan type');
        }
    });
}