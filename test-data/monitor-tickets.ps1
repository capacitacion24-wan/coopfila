# monitor-tickets.ps1 - Monitor avanzado en PowerShell

$API_URL = "http://localhost:8080/api"
$CHAT_ID = "1634964503"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   MONITOR TIEMPO REAL - TICKETERO" -ForegroundColor Cyan  
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "🔍 Monitoreando cambios cada 5 segundos..." -ForegroundColor Yellow
Write-Host "📱 Chat ID: $CHAT_ID" -ForegroundColor Green
Write-Host "🤖 Bot configurado correctamente" -ForegroundColor Green
Write-Host ""
Write-Host "Presiona Ctrl+C para detener" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Cyan

$previousState = ""

while ($true) {
    Clear-Host
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "   MONITOR TIEMPO REAL - TICKETERO" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "🕐 $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
    Write-Host ""

    try {
        # Obtener tickets activos
        $tickets = Invoke-RestMethod -Uri "$API_URL/admin/tickets/active" -Method Get
        
        Write-Host "🎫 TICKETS ACTIVOS ($($tickets.Count)):" -ForegroundColor Yellow
        Write-Host "----------------------------------------" -ForegroundColor Gray
        
        if ($tickets.Count -eq 0) {
            Write-Host "   📭 No hay tickets activos" -ForegroundColor Gray
        } else {
            foreach ($ticket in $tickets) {
                $statusColor = switch ($ticket.status) {
                    "EN_ESPERA" { "White" }
                    "PROXIMO" { "Yellow" }
                    "ATENDIENDO" { "Green" }
                    "COMPLETADO" { "Blue" }
                    default { "Gray" }
                }
                
                $advisorInfo = if ($ticket.assignedAdvisorName) { 
                    "👨‍💼 $($ticket.assignedAdvisorName) (Mod: $($ticket.assignedModuleNumber))" 
                } else { 
                    "⏳ Sin asignar" 
                }
                
                Write-Host "   📋 $($ticket.numero) | 👤 $($ticket.clienteNombre) | 🏢 $($ticket.queueType)" -ForegroundColor White
                Write-Host "      🔄 $($ticket.status) | 📍 Pos: $($ticket.positionInQueue) | ⏱️ $($ticket.estimatedWaitMinutes)min | $advisorInfo" -ForegroundColor $statusColor
                Write-Host ""
            }
        }

        # Obtener asesores
        $advisors = Invoke-RestMethod -Uri "$API_URL/admin/advisors/available" -Method Get
        
        Write-Host "👥 ASESORES DISPONIBLES ($($advisors.Count)):" -ForegroundColor Yellow
        Write-Host "----------------------------------------" -ForegroundColor Gray
        
        if ($advisors.Count -eq 0) {
            Write-Host "   👥 Todos los asesores ocupados" -ForegroundColor Red
        } else {
            foreach ($advisor in $advisors) {
                Write-Host "   👤 $($advisor.name) | 🏢 Módulo $($advisor.moduleNumber) | 📊 $($advisor.assignedTicketsCount) tickets" -ForegroundColor Green
            }
        }
        Write-Host ""

        # Dashboard resumen
        $dashboard = Invoke-RestMethod -Uri "$API_URL/admin/dashboard" -Method Get
        Write-Host "📊 RESUMEN GENERAL:" -ForegroundColor Yellow
        Write-Host "----------------------------------------" -ForegroundColor Gray
        Write-Host "   📈 Total Tickets Activos: $($dashboard.totalActiveTickets)" -ForegroundColor White
        Write-Host "   👥 Asesores Disponibles: $($dashboard.totalAvailableAdvisors)" -ForegroundColor White
        Write-Host ""

        # Detectar cambios
        $currentState = ($tickets | ConvertTo-Json -Compress)
        if ($previousState -ne "" -and $currentState -ne $previousState) {
            Write-Host "🆕 CAMBIO DETECTADO!" -ForegroundColor Red -BackgroundColor Yellow
            Write-Host "   📝 Estado actualizado - revisa Telegram" -ForegroundColor Yellow
            
            # Log del cambio
            Add-Content -Path "monitor-log.txt" -Value "[$(Get-Date)] CAMBIO DE ESTADO DETECTADO"
        }
        $previousState = $currentState

    } catch {
        Write-Host "❌ Error consultando API: $($_.Exception.Message)" -ForegroundColor Red
        Add-Content -Path "monitor-log.txt" -Value "[$(Get-Date)] ERROR: $($_.Exception.Message)"
    }

    Write-Host "⏱️ Próxima actualización en 5 segundos..." -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    Start-Sleep -Seconds 5
}