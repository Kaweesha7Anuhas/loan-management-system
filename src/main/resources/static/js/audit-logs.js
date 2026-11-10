function loadAuditLogs() {
    $('#auditTableBody').html('<tr><td colspan="5" class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></td></tr>');

    $.get('/api/audit-logs?size=50', function(res) {
        let html = '';
        if (res.data.empty) {
            html = '<tr><td colspan="5" class="text-center py-5 text-muted">No audit logs found</td></tr>';
        } else {
            res.data.content.forEach(log => {
                // Format timestamp to a readable string
                const date = new Date(log.createdAt).toLocaleString();
                
                html += `
                    <tr>
                        <td class="ps-4 text-muted small">${date}</td>
                        <td class="fw-bold">${log.username}</td>
                        <td><span class="badge bg-secondary">${log.action}</span></td>
                        <td>${log.entityType || '-'}</td>
                        <td class="text-muted">${log.description || '-'}</td>
                    </tr>
                `;
            });
        }
        $('#auditTableBody').html(html);
    });
}