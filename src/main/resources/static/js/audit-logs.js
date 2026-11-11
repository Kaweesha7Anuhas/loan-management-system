// audit-logs.js
// Read-only Audit Log viewer. Displays the last 50 entries from the server.
// Depends on: api.js (jQuery setup, CSRF, 401 handling). No endpoint changes.

'use strict';

/* ──────────────────────────────────────────────────────────────────────────
 * Local helpers (prefixed to avoid clashing with other feature files)
 * ──────────────────────────────────────────────────────────────────────── */

function escapeAuditHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * Safely format an audit-log timestamp.
 * Returns { iso, display } or null when the input is not a valid date.
 */
function formatAuditTimestamp(raw) {
    if (!raw) return null;
    var d = new Date(raw);
    if (isNaN(d.getTime())) return null;
    return {
        iso: String(raw),
        display: d.toLocaleString()
    };
}

/**
 * Map a known action name to a themed status class.
 * Unknown actions fall back to neutral so behaviour matches the original.
 */
function auditActionClass(action) {
    var name = String(action || '').toUpperCase();

    // Order matters: test specific verbs before generic ones.
    if (/^(CREATE|ADD|INSERT|APPROVE|DISBURSE|PAYMENT|PAY)/.test(name)) return 'status-success';
    if (/^(UPDATE|EDIT|MODIFY)/.test(name))                             return 'status-info';
    if (/^(DELETE|REMOVE|REJECT)/.test(name))                           return 'status-danger';
    if (/^(LOGIN|LOGOUT|AUTH|SIGN)/.test(name))                         return 'status-primary';
    return 'status-neutral';
}

/* ──────────────────────────────────────────────────────────────────────────
 * Main loader
 * ──────────────────────────────────────────────────────────────────────── */

function loadAuditLogs() {
    var $tbody = $('#auditTableBody');
    if (!$tbody.length) return;

    $tbody.html(
        '<tr><td colspan="5" class="text-center py-4">' +
            '<div class="spinner-border text-primary" role="status"></div>' +
        '</td></tr>'
    );

    $.get('/api/audit-logs', { size: 50 })
        .done(function (res) {
            var data = (res && res.data) || {};
            var content = data.content || [];
            var rows = '';

            if (!content.length) {
                rows =
                    '<tr><td colspan="5" class="border-0">' +
                        '<div class="empty-state">' +
                            '<i class="bi bi-shield-check"></i>' +
                            'No audit logs found' +
                        '</div>' +
                    '</td></tr>';
            } else {
                content.forEach(function (log) {
                    var ts = formatAuditTimestamp(log.createdAt);
                    var tsCell = ts
                        ? '<time class="text-muted small" datetime="' + escapeAuditHtml(ts.iso) + '">' +
                              escapeAuditHtml(ts.display) +
                          '</time>'
                        : '<span class="text-muted small">-</span>';

                    var action = log.action ? String(log.action) : '-';
                    var badge = '<span class="status-badge ' + auditActionClass(log.action) + '">' +
                        escapeAuditHtml(action) +
                    '</span>';

                    rows +=
                        '<tr>' +
                            '<td class="ps-4">' + tsCell + '</td>' +
                            '<td class="fw-semibold">' + escapeAuditHtml(log.username || '-') + '</td>' +
                            '<td>' + badge + '</td>' +
                            '<td>' + escapeAuditHtml(log.entityType || '-') + '</td>' +
                            '<td class="text-muted">' + escapeAuditHtml(log.description || '-') + '</td>' +
                        '</tr>';
                });
            }

            $tbody.html(rows);
        })
        .fail(function () {
            $tbody.html(
                '<tr><td colspan="5" class="text-center py-5 text-danger">' +
                    'Failed to load audit logs' +
                '</td></tr>'
            );
        });
}