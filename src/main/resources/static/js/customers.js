// customers.js
// Customer Management: listing + search + pagination, create / edit modal.
// Depends on: api.js (jQuery setup, CSRF, formatCurrency), Bootstrap 5 modals.

'use strict';

let currentPage = 0;
let currentSearch = '';
let searchTimeout = null;

// The customer table body always renders 7 cells:
//   Code | Name | NIC | Phone | Email | Status | Actions
// IMPORTANT: <thead> in index.html must also have 7 <th> (Email is currently missing there).
const CUSTOMER_COLSPAN = 7;

/* ──────────────────────────────────────────────────────────────────────
 * Helpers
 * ──────────────────────────────────────────────────────────────────── */

function escapeCustomerHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function openCustomerModal() {
    const el = document.getElementById('customerModal');
    if (!el) return;
    if (window.bootstrap && window.bootstrap.Modal) {
        window.bootstrap.Modal.getOrCreateInstance(el).show();
        return;
    }
    if (window.jQuery && $.fn && $.fn.modal) {
        $(el).modal('show'); // legacy fallback
    }
}

function closeCustomerModal() {
    const el = document.getElementById('customerModal');
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
 * List + render
 * ──────────────────────────────────────────────────────────────────── */

function loadCustomers(page, search) {
    currentPage = page;
    currentSearch = search == null ? '' : search;

    $('#customerTableBody').html(
        `<tr><td colspan="${CUSTOMER_COLSPAN}" class="text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
        </td></tr>`
    );

    $.get('/api/customers', {
        page: page,
        size: 10,
        search: currentSearch,
        sortBy: 'createdAt',
        sortDir: 'desc'
    })
        .done(function (res) {
            const data = res.data || {};
            const content = data.content || [];
            let html = '';

            if (!content.length) {
                html = `<tr><td colspan="${CUSTOMER_COLSPAN}" class="border-0">
                    <div class="empty-state">
                        <i class="bi bi-people"></i>
                        No customers found
                    </div>
                </td></tr>`;
            } else {
                content.forEach(c => {
                    const statusCls = c.customerStatus === 'ACTIVE'
                        ? 'status-success'
                        : 'status-danger';

                    html += `
                        <tr>
                            <td class="ps-4 fw-semibold text-primary">${escapeCustomerHtml(c.customerCode)}</td>
                            <td>${escapeCustomerHtml(c.fullName)}</td>
                            <td>${escapeCustomerHtml(c.nicNumber)}</td>
                            <td>${escapeCustomerHtml(c.phoneNumber)}</td>
                            <td>${escapeCustomerHtml(c.email)}</td>
                            <td>
                                <span class="status-badge ${statusCls}">${escapeCustomerHtml(c.customerStatus)}</span>
                            </td>
                            <td class="text-end pe-4">
                                <button class="btn btn-light text-primary btn-icon"
                                        title="Edit customer"
                                        onclick="editCustomer(${Number(c.id)})">
                                    <i class="bi bi-pencil"></i>
                                </button>
                            </td>
                        </tr>
                    `;
                });
            }
            $('#customerTableBody').html(html);
            renderPagination(data);
        })
        .fail(function () {
            $('#customerTableBody').html(
                `<tr><td colspan="${CUSTOMER_COLSPAN}" class="text-center py-5 text-danger">
                    Failed to load customers
                </td></tr>`
            );
            renderPagination({
                page: 0, size: 10, totalElements: 0, totalPages: 1,
                first: true, last: true, empty: true
            });
        });
}

/* ──────────────────────────────────────────────────────────────────────
 * Pagination
 * ──────────────────────────────────────────────────────────────────── */

function renderPagination(pageData) {
    const totalElements = pageData.totalElements || 0;
    const page = pageData.page || 0;
    const size = pageData.size || 10;
    const totalPages = pageData.totalPages || Math.max(1, Math.ceil(totalElements / size));

    const start = totalElements === 0 ? 0 : page * size + 1;
    const end = Math.min((page + 1) * size, totalElements);

    $('#customerPaginationInfo').text(
        `Showing ${start} to ${end} of ${totalElements} entries`
    );

    if (totalPages <= 1) {
        $('#customerPagination').empty();
        return;
    }

    let html = '';

    // Prev
    html += `<li class="page-item ${pageData.first ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${page - 1}">Prev</a>
             </li>`;

    // Page-number window with ellipses
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

    // Next
    html += `<li class="page-item ${pageData.last ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${page + 1}">Next</a>
             </li>`;

    $('#customerPagination').html(html);
}

/* ──────────────────────────────────────────────────────────────────────
 * Event wiring
 * ──────────────────────────────────────────────────────────────────── */

$(function () {
    // Debounced search
    $('#customerSearch').on('input', function () {
        const val = $(this).val();
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => { loadCustomers(0, val); }, 400);
    });

    // Enter inside the customer form saves the record instead of reloading.
    $('#customerForm').on('submit', function (e) {
        e.preventDefault();
        saveCustomer();
    });
});

// Delegated pagination handler — safe against HTML injection in search text.
$(document).on('click', '#customerPagination a[data-page]', function (e) {
    e.preventDefault();
    const $li = $(this).closest('.page-item');
    if ($li.hasClass('disabled') || $li.hasClass('active')) return;
    const target = parseInt($(this).attr('data-page'), 10);
    if (!isNaN(target) && target >= 0) {
        loadCustomers(target, currentSearch);
    }
});

/* ──────────────────────────────────────────────────────────────────────
 * Form: reset / edit / save
 * ──────────────────────────────────────────────────────────────────── */

function clearCustomerForm() {
    const form = document.getElementById('customerForm');
    if (form) form.reset();
    $('#customerId').val('');
    $('#customerModalTitle').text('New Customer');
    $('#nicNumber').prop('disabled', false);
}

function editCustomer(id) {
    $.get(`/api/customers/${id}`)
        .done(function (res) {
            const c = res.data || {};
            $('#customerId').val(c.id);
            $('#firstName').val(c.firstName);
            $('#lastName').val(c.lastName);
            $('#nicNumber').val(c.nicNumber).prop('disabled', true); // NIC is immutable
            $('#email').val(c.email);
            $('#phoneNumber').val(c.phoneNumber);
            $('#dateOfBirth').val(c.dateOfBirth);
            $('#address').val(c.address);
            $('#city').val(c.city);
            $('#occupation').val(c.occupation);
            $('#monthlyIncome').val(c.monthlyIncome);

            $('#customerModalTitle').text('Edit Customer');
            openCustomerModal();
        })
        .fail(function (xhr) {
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                || 'Failed to load customer';
            alert(msg);
        });
}

function saveCustomer() {
    const id = $('#customerId').val();
    const isEdit = id !== '';

    // Client-side guard: HTML `required` attributes are not enforced on
    // an `onclick` button, so we invoke the browser's native constraint
    // validation here.
    const form = document.getElementById('customerForm');
    if (form && typeof form.reportValidity === 'function' && !form.checkValidity()) {
        form.reportValidity();
        return;
    }

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

    const $btn = $('#saveCustomerBtn').prop('disabled', true).text('Saving...');

    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(payload)
    })
        .done(function () {
            closeCustomerModal();
            $btn.prop('disabled', false).text('Save Customer');
            loadCustomers(currentPage, currentSearch);
        })
        .fail(function (xhr) {
            $btn.prop('disabled', false).text('Save Customer');

            if (xhr.responseJSON && xhr.responseJSON.fieldErrors) {
                const fieldErrors = xhr.responseJSON.fieldErrors;
                let errorMsg = 'Please fix the following errors:\n\n';
                Object.keys(fieldErrors).forEach(field => {
                    errorMsg += `• ${field}: ${fieldErrors[field]}\n`;
                });
                alert(errorMsg);
                return;
            }
            const msg = (xhr.responseJSON && xhr.responseJSON.message)
                || 'Error saving customer';
            alert(msg);
        });
}