let currentLoanPage = 0;
let currentLoanSearch = '';
let loanSearchTimeout = null;
let activeLoanId = null;

function loadLoans(page, search) {
    currentLoanPage = page;
    currentLoanSearch = search;

    $('#loanTableBody').html('<tr><td colspan="6" class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></td></tr>');

    $.get(`/api/loans?page=${page}&size=10&search=${search}&sortBy=createdAt&sortDir=desc`)
        .done(function(res) {
            const data = res.data;
            let html = '';

            if (data.empty) {
                html = '<tr><td colspan="6" class="text-center py-5 text-muted">No loans found</td></tr>';
            } else {
                data.content.forEach(l => {
                    const badgeClass = l.loanStatus === 'ACTIVE' ? 'bg-success' : (l.loanStatus === 'COMPLETED' ? 'bg-secondary' : 'bg-danger');
                    
                    html += `
                        <tr>
                            <td class="ps-4 fw-bold text-primary">${l.loanAccountNumber}</td>
                            <td>${l.customerFullName}</td>
                            <td>${formatCurrency(l.principalAmount)}</td>
                            <td class="text-danger fw-bold">${formatCurrency(l.outstandingBalance)}</td>
                            <td><span class="badge ${badgeClass} rounded-pill px-3">${l.loanStatus}</span></td>
                            <td class="text-end pe-4">
                                <button class="btn btn-sm btn-primary" onclick="viewLoan(${l.id})">View Details</button>
                            </td>
                        </tr>
                    `;
                });
            }
            $('#loanTableBody').html(html);
            renderLoanPagination(data);
        });
}

function renderLoanPagination(pageData) {
    $('#loanPaginationInfo').text(`Showing ${pageData.empty ? 0 : pageData.page * pageData.size + 1} to ${Math.min((pageData.page + 1) * pageData.size, pageData.totalElements)} of ${pageData.totalElements} entries`);
    let html = '';
    html += `<li class="page-item ${pageData.first ? 'disabled' : ''}"><a class="page-link" href="#" onclick="event.preventDefault(); ${!pageData.first ? `loadLoans(${currentLoanPage - 1}, '${currentLoanSearch}')` : ''}">Prev</a></li>`;
    html += `<li class="page-item ${pageData.last ? 'disabled' : ''}"><a class="page-link" href="#" onclick="event.preventDefault(); ${!pageData.last ? `loadLoans(${currentLoanPage + 1}, '${currentLoanSearch}')` : ''}">Next</a></li>`;
    $('#loanPagination').html(html);
}

$(document).ready(function() {
    $('#loanSearch').on('input', function() {
        const val = $(this).val();
        clearTimeout(loanSearchTimeout);
        loanSearchTimeout = setTimeout(() => { loadLoans(0, val); }, 400);
    });
});

// View Detailed Loan Schedule
function viewLoan(id) {
    activeLoanId = id;
    $('#view-loans').hide();
    $('#view-loan-details').fadeIn();

    // Fetch Loan Details
    $.get(`/api/loans/${id}`, function(res) {
        const l = res.data;
        $('#detail-loan-account').text(l.loanAccountNumber);
        $('#detail-customer-name').text(l.customerFullName);
        $('#detail-principal').text(formatCurrency(l.principalAmount));
        $('#detail-rate').text(l.annualInterestRate + '%');
        $('#detail-payable').text(formatCurrency(l.totalPayable));
        $('#detail-outstanding').text(formatCurrency(l.outstandingBalance));
        $('#detail-loan-status').text(l.loanStatus);
        
        if (l.loanStatus !== 'ACTIVE') {
            $('#btnRecordPayment').hide();
        }
    });

    // Fetch Schedule
    $.get(`/api/loans/${id}/schedule`, function(res) {
        let html = '';
        res.data.forEach(s => {
            let badge = 'bg-secondary';
            if (s.paymentStatus === 'PAID') badge = 'bg-success';
            else if (s.paymentStatus === 'PARTIALLY_PAID') badge = 'bg-warning text-dark';
            else if (s.paymentStatus === 'OVERDUE') badge = 'bg-danger';

            html += `
                <tr>
                    <td class="ps-4">${s.instalmentNumber}</td>
                    <td>${s.dueDate}</td>
                    <td class="fw-bold">${formatCurrency(s.instalmentAmount)}</td>
                    <td>${formatCurrency(s.principalAmount)}</td>
                    <td>${formatCurrency(s.interestAmount)}</td>
                    <td class="text-success">${formatCurrency(s.paidAmount)}</td>
                    <td class="text-danger">${formatCurrency(s.remainingAmount)}</td>
                    <td><span class="badge ${badge} rounded-pill">${s.paymentStatus}</span></td>
                </tr>
            `;
        });
        $('#scheduleTableBody').html(html);
    });
}

// Open Payment Modal
function openPaymentModal() {
    $('#paymentModal').modal('show');
    $('#payAmount').val('');
    $('#payRef').val('');
    
    const outstanding = $('#detail-outstanding').text().replace('LKR ', '').replace(/,/g, '');
    $('#payAmount').attr('max', outstanding);
    $('#payOutstandingHint').text('Max: ' + formatCurrency(outstanding));
}

// Submit Payment
function submitPayment() {
    const amount = $('#payAmount').val();
    if (!amount || amount <= 0) { alert('Enter a valid amount'); return; }

    const payload = {
        loanId: activeLoanId,
        paymentAmount: amount,
        paymentMethod: $('#payMethod').val(),
        transactionReference: $('#payRef').val() || null
    };

    $('#payBtn').prop('disabled', true).text('Processing...');

    $.ajax({
        url: '/api/repayments',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function(res) {
            $('#paymentModal').modal('hide');
            $('#payBtn').prop('disabled', false).text('Record Payment');
            alert('Payment recorded! Receipt: ' + res.data.receiptNumber);
            
            // Reload the details to show new balances
            viewLoan(activeLoanId); 
        },
        error: function(xhr) {
            $('#payBtn').prop('disabled', false).text('Record Payment');
            alert(xhr.responseJSON ? xhr.responseJSON.message : 'Error recording payment');
        }
    });
}