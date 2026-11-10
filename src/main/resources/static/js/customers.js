let currentPage = 0;
let currentSearch = '';
let searchTimeout = null;

// Load and render the table
function loadCustomers(page, search) {
    currentPage = page;
    currentSearch = search;

    $('#customerTableBody').html('<tr><td colspan="7" class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></td></tr>');

    $.get(`/api/customers?page=${page}&size=10&search=${search}&sortBy=createdAt&sortDir=desc`)
        .done(function(res) {
            const data = res.data;
            let html = '';

            if (data.empty) {
                html = '<tr><td colspan="7" class="text-center py-5 text-muted">No customers found</td></tr>';
            } else {
                data.content.forEach(c => {
                    const badgeClass = c.customerStatus === 'ACTIVE' ? 'bg-success' : 'bg-danger';
                    
                    html += `
                        <tr>
                            <td class="ps-4 fw-bold text-primary">${c.customerCode}</td>
                            <td>${c.fullName}</td>
                            <td>${c.nicNumber}</td>
                            <td>${c.phoneNumber}</td>
                            <td>${c.email}</td>
                            <td><span class="badge ${badgeClass} rounded-pill px-3">${c.customerStatus}</span></td>
                            <td class="text-end pe-4">
                                <button class="btn btn-sm btn-light text-primary" onclick="editCustomer(${c.id})"><i class="bi bi-pencil"></i></button>
                            </td>
                        </tr>
                    `;
                });
            }
            $('#customerTableBody').html(html);
            renderPagination(data);
        })
        .fail(function() {
            $('#customerTableBody').html('<tr><td colspan="7" class="text-center text-danger">Failed to load data</td></tr>');
        });
}

// Render pagination buttons
function renderPagination(pageData) {
    $('#customerPaginationInfo').text(`Showing ${pageData.empty ? 0 : pageData.page * pageData.size + 1} to ${Math.min((pageData.page + 1) * pageData.size, pageData.totalElements)} of ${pageData.totalElements} entries`);
    
    let html = '';
    
    // Prev button
    html += `<li class="page-item ${pageData.first ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="event.preventDefault(); ${!pageData.first ? `loadCustomers(${currentPage - 1}, '${currentSearch}')` : ''}">Prev</a>
             </li>`;

    // Next button
    html += `<li class="page-item ${pageData.last ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="event.preventDefault(); ${!pageData.last ? `loadCustomers(${currentPage + 1}, '${currentSearch}')` : ''}">Next</a>
             </li>`;

    $('#customerPagination').html(html);
}

// Search logic (Debounced)
$(document).ready(function() {
    $('#customerSearch').on('input', function() {
        const val = $(this).val();
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            loadCustomers(0, val);
        }, 400); // Wait 400ms after typing stops
    });
});

// Form Reset
function clearCustomerForm() {
    $('#customerForm')[0].reset();
    $('#customerId').val('');
    $('#customerModalTitle').text('New Customer');
    $('#nicNumber').prop('disabled', false);
}

// Edit Logic
function editCustomer(id) {
    $.get(`/api/customers/${id}`)
        .done(function(res) {
            const c = res.data;
            $('#customerId').val(c.id);
            $('#firstName').val(c.firstName);
            $('#lastName').val(c.lastName);
            $('#nicNumber').val(c.nicNumber).prop('disabled', true); // NIC cannot be edited
            $('#email').val(c.email);
            $('#phoneNumber').val(c.phoneNumber);
            $('#dateOfBirth').val(c.dateOfBirth);
            $('#address').val(c.address);
            $('#city').val(c.city);
            $('#occupation').val(c.occupation);
            $('#monthlyIncome').val(c.monthlyIncome);
            
            $('#customerModalTitle').text('Edit Customer');
            $('#customerModal').modal('show');
        });
}

// Save (Create or Update)
function saveCustomer() {
    const id = $('#customerId').val();
    const isEdit = id !== '';
    
    const payload = {
        firstName: $('#firstName').val(),
        lastName: $('#lastName').val(),
        email: $('#email').val(),
        phoneNumber: $('#phoneNumber').val(),
        dateOfBirth: $('#dateOfBirth').val() || null,
        address: $('#address').val(),
        city: $('#city').val(),
        occupation: $('#occupation').val(),
        monthlyIncome: $('#monthlyIncome').val() || null
    };

    if (!isEdit) {
        payload.nicNumber = $('#nicNumber').val();
    }

    const method = isEdit ? 'PUT' : 'POST';
    const url = isEdit ? `/api/customers/${id}` : '/api/customers';

    $('#saveCustomerBtn').prop('disabled', true).text('Saving...');

    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(payload),
        success: function() {
            $('#customerModal').modal('hide');
            $('#saveCustomerBtn').prop('disabled', false).text('Save Customer');
            loadCustomers(currentPage, currentSearch); // Reload table
        },
                error: function(xhr) {
            $('#saveCustomerBtn').prop('disabled', false).text('Save Customer');
            
            if (xhr.responseJSON && xhr.responseJSON.fieldErrors) {
                // Show the exact field validation errors
                let errorMsg = "Please fix the following errors:\n\n";
                for (const [field, message] of Object.entries(xhr.responseJSON.fieldErrors)) {
                    errorMsg += `• ${field}: ${message}\n`;
                }
                alert(errorMsg);
            } else {
                const msg = xhr.responseJSON ? xhr.responseJSON.message : 'Error saving customer';
                alert(msg);
            }
        }
    });
}