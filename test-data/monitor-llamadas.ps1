# monitor-llamadas.ps1 - Monitor simple para orden de llamadas

$API_URL = "http://localhost:8080/api"

while ($true) {
    Clear-Host
    
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "          MONITOR DE LLAMADAS - TICKETERO" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "🕐 $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
    Write-Host ""
    
    Write-Host "🎫 TICKETS PARA LLAMAR (en orden):" -ForegroundColor Yellow
    Write-Host "───────────────────────────────────────────────────────" -ForegroundColor Gray
    
    try {
        $tickets = Invoke-RestMethod -Uri "$API_URL/admin/tickets/active" -Method Get
        
        if ($tickets.Count -eq 0) {
            Write-Host "  📭 No hay tickets pendientes" -ForegroundColor Gray
        } else {
            $sortedTickets = $tickets | Sort-Object positionInQueue
            
            foreach ($ticket in $sortedTickets) {
                $statusIcon = switch ($ticket.status) {
                    "EN_ESPERA" { "⏳" }
                    "PROXIMO" { "🟡" }
                    "ATENDIENDO" { "🟢" }
                    default { "📋" }
                }
                
                $color = switch ($ticket.status) {
                    "PROXIMO" { "Yellow" }
                    "ATENDIENDO" { "Green" }
                    default { "White" }
                }
                
                Write-Host "  $statusIcon $($ticket.numero) | 👤 $($ticket.clienteNombre) | 🏢 $($ticket.queueType) | Pos: $($ticket.positionInQueue) | $($ticket.status)" -ForegroundColor $color
            }
        }
        
    } catch {
        Write-Host "  ❌ Error consultando tickets: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "⏱️ Actualizando en 5 segundos... (Ctrl+C para salir)" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    
    Start-Sleep -Seconds 5
}