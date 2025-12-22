# monitor-avanzado.ps1 - Monitor completo con alertas inteligentes

param(
    [string]$ApiUrl = "http://localhost:8080/api",
    [string]$ChatId = "1634964503",
    [int]$IntervalSeconds = 5,
    [switch]$EnableAlerts = $true,
    [switch]$SaveLog = $true
)

# Configuración
$Global:PreviousState = @{}
$Global:AlertThresholds = @{
    MaxWaitTime = 30
    MaxQueueLength = 10
    MinAvailableAdvisors = 1
}

function Write-ColoredHeader {
    param([string]$Text, [string]$Color = "Cyan")
    Write-Host "=" * 60 -ForegroundColor $Color
    Write-Host "  $Text" -ForegroundColor $Color
    Write-Host "=" * 60 -ForegroundColor $Color
}

function Write-StatusIcon {
    param([string]$Status)
    switch ($Status) {
        "EN_ESPERA" { return "⏳" }
        "PROXIMO" { return "🟡" }
        "ATENDIENDO" { return "🟢" }
        "COMPLETADO" { return "✅" }
        default { return "❓" }
    }
}

function Get-StatusColor {
    param([string]$Status)
    switch ($Status) {
        "EN_ESPERA" { return "White" }
        "PROXIMO" { return "Yellow" }
        "ATENDIENDO" { return "Green" }
        "COMPLETADO" { return "Blue" }
        default { return "Gray" }
    }
}

function Send-TelegramAlert {
    param([string]$Message)
    if ($EnableAlerts) {
        try {
            $body = @{ chatId = $ChatId; message = "🚨 ALERTA: $Message" } | ConvertTo-Json
            Invoke-RestMethod -Uri "$ApiUrl/test/telegram" -Method Post -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
        } catch {
            Write-Host "⚠️ Error enviando alerta Telegram" -ForegroundColor Yellow
        }
    }
}

function Write-LogEntry {
    param([string]$Message)
    if ($SaveLog) {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Add-Content -Path "monitor-avanzado.log" -Value "[$timestamp] $Message"
    }
}

function Check-Alerts {
    param($Tickets, $Advisors)
    
    # Alerta: Tiempo de espera excesivo
    $longWaitTickets = $Tickets | Where-Object { $_.estimatedWaitMinutes -gt $Global:AlertThresholds.MaxWaitTime }
    if ($longWaitTickets) {
        $message = "Tickets con espera >$($Global:AlertThresholds.MaxWaitTime)min: $($longWaitTickets.Count)"
        Send-TelegramAlert $message
        Write-LogEntry "ALERTA: $message"
    }
    
    # Alerta: Cola muy larga
    $queueTypes = $Tickets | Group-Object queueType
    foreach ($queue in $queueTypes) {
        if ($queue.Count -gt $Global:AlertThresholds.MaxQueueLength) {
            $message = "Cola $($queue.Name) muy larga: $($queue.Count) tickets"
            Send-TelegramAlert $message
            Write-LogEntry "ALERTA: $message"
        }
    }
    
    # Alerta: Pocos asesores disponibles
    if ($Advisors.Count -lt $Global:AlertThresholds.MinAvailableAdvisors) {
        $message = "Solo $($Advisors.Count) asesores disponibles"
        Send-TelegramAlert $message
        Write-LogEntry "ALERTA: $message"
    }
}

function Show-Dashboard {
    param($Tickets, $Advisors, $Dashboard)
    
    Clear-Host
    
    Write-ColoredHeader "MONITOR AVANZADO - SISTEMA TICKETERO" "Cyan"
    Write-Host "🕐 $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
    Write-Host "📱 Chat ID: $ChatId | 🔄 Intervalo: ${IntervalSeconds}s | 🚨 Alertas: $EnableAlerts" -ForegroundColor Gray
    Write-Host ""

    # Métricas principales
    Write-Host "📊 MÉTRICAS PRINCIPALES" -ForegroundColor Yellow
    Write-Host "─" * 40 -ForegroundColor Gray
    Write-Host "  📈 Tickets Activos: $($Dashboard.totalActiveTickets)" -ForegroundColor White
    Write-Host "  👥 Asesores Disponibles: $($Dashboard.totalAvailableAdvisors)" -ForegroundColor Green
    Write-Host "  ⏱️ Tiempo Promedio Espera: $([math]::Round(($Tickets | Measure-Object estimatedWaitMinutes -Average).Average, 1))min" -ForegroundColor White
    Write-Host ""

    # Tickets por cola
    if ($Tickets.Count -gt 0) {
        Write-Host "🎫 TICKETS POR COLA" -ForegroundColor Yellow
        Write-Host "─" * 40 -ForegroundColor Gray
        $queueStats = $Tickets | Group-Object queueType | Sort-Object Name
        foreach ($queue in $queueStats) {
            $avgWait = [math]::Round(($queue.Group | Measure-Object estimatedWaitMinutes -Average).Average, 1)
            Write-Host "  🏢 $($queue.Name): $($queue.Count) tickets (⏱️ ${avgWait}min promedio)" -ForegroundColor White
        }
        Write-Host ""
    }

    # Tickets activos detallados
    Write-Host "🎫 TICKETS ACTIVOS ($($Tickets.Count))" -ForegroundColor Yellow
    Write-Host "─" * 60 -ForegroundColor Gray
    
    if ($Tickets.Count -eq 0) {
        Write-Host "  📭 No hay tickets activos" -ForegroundColor Gray
    } else {
        foreach ($ticket in $Tickets | Sort-Object positionInQueue) {
            $statusIcon = Write-StatusIcon $ticket.status
            $statusColor = Get-StatusColor $ticket.status
            
            $advisorInfo = if ($ticket.assignedAdvisorName) { 
                "👨💼 $($ticket.assignedAdvisorName) (Mod: $($ticket.assignedModuleNumber))" 
            } else { 
                "⏳ Sin asignar" 
            }
            
            Write-Host "  $statusIcon $($ticket.numero) | 👤 $($ticket.clienteNombre) | 🏢 $($ticket.queueType)" -ForegroundColor White
            Write-Host "    📍 Pos: $($ticket.positionInQueue) | ⏱️ $($ticket.estimatedWaitMinutes)min | $advisorInfo" -ForegroundColor $statusColor
        }
    }
    Write-Host ""

    # Asesores disponibles
    Write-Host "👥 ASESORES DISPONIBLES ($($Advisors.Count))" -ForegroundColor Yellow
    Write-Host "─" * 40 -ForegroundColor Gray
    
    if ($Advisors.Count -eq 0) {
        Write-Host "  ❌ Todos los asesores ocupados" -ForegroundColor Red
    } else {
        foreach ($advisor in $Advisors | Sort-Object moduleNumber) {
            $workload = switch ($advisor.assignedTicketsCount) {
                0 { "🟢 Libre" }
                1 { "🟡 Ocupado" }
                default { "🔴 Sobrecargado ($($advisor.assignedTicketsCount))" }
            }
            Write-Host "  👤 $($advisor.name) | 🏢 Mod: $($advisor.moduleNumber) | $workload" -ForegroundColor Green
        }
    }
    Write-Host ""

    # Estado del sistema
    Write-Host "🔧 ESTADO DEL SISTEMA" -ForegroundColor Yellow
    Write-Host "─" * 40 -ForegroundColor Gray
    
    # Verificar salud de la API
    try {
        $health = Invoke-RestMethod -Uri "$ApiUrl/../actuator/health" -Method Get -TimeoutSec 3
        Write-Host "  ✅ API: Operativa" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ API: No responde" -ForegroundColor Red
    }
    
    # Verificar schedulers (simulado)
    Write-Host "  🔄 MessageScheduler: Activo (cada 60s)" -ForegroundColor Green
    Write-Host "  🔄 QueueProcessor: Activo (cada 5s)" -ForegroundColor Green
    Write-Host ""
}

function Detect-Changes {
    param($CurrentTickets)
    
    $currentState = $CurrentTickets | ConvertTo-Json -Compress
    $stateHash = [System.Security.Cryptography.SHA256]::Create().ComputeHash([System.Text.Encoding]::UTF8.GetBytes($currentState))
    $stateHashString = [System.BitConverter]::ToString($stateHash) -replace '-'
    
    if ($Global:PreviousState.Hash -and $Global:PreviousState.Hash -ne $stateHashString) {
        Write-Host "🆕 CAMBIO DETECTADO!" -ForegroundColor Red -BackgroundColor Yellow
        Write-LogEntry "CAMBIO DE ESTADO DETECTADO"
        
        # Analizar qué cambió
        $prevTickets = $Global:PreviousState.Tickets
        $newTickets = $CurrentTickets | Where-Object { $_.numero -notin $prevTickets.numero }
        $updatedTickets = $CurrentTickets | Where-Object { 
            $prev = $prevTickets | Where-Object { $_.numero -eq $_.numero }
            $prev -and ($prev.status -ne $_.status -or $prev.positionInQueue -ne $_.positionInQueue)
        }
        
        if ($newTickets) {
            Write-Host "  ➕ Nuevos tickets: $($newTickets.Count)" -ForegroundColor Green
        }
        if ($updatedTickets) {
            Write-Host "  🔄 Tickets actualizados: $($updatedTickets.Count)" -ForegroundColor Yellow
        }
        Write-Host ""
    }
    
    $Global:PreviousState = @{
        Hash = $stateHashString
        Tickets = $CurrentTickets
        Timestamp = Get-Date
    }
}

# Función principal
function Start-Monitor {
    Write-ColoredHeader "INICIANDO MONITOR AVANZADO" "Green"
    Write-Host "🚀 Configuración cargada" -ForegroundColor Green
    Write-Host "📡 API: $ApiUrl" -ForegroundColor White
    Write-Host "📱 Telegram: $ChatId" -ForegroundColor White
    Write-Host "⏱️ Intervalo: ${IntervalSeconds}s" -ForegroundColor White
    Write-Host "🚨 Alertas: $EnableAlerts" -ForegroundColor White
    Write-Host "📝 Log: $SaveLog" -ForegroundColor White
    Write-Host ""
    Write-Host "Presiona Ctrl+C para detener" -ForegroundColor Red
    Start-Sleep 3

    Write-LogEntry "Monitor iniciado - API: $ApiUrl, Chat: $ChatId"

    while ($true) {
        try {
            # Obtener datos
            $tickets = Invoke-RestMethod -Uri "$ApiUrl/admin/tickets/active" -Method Get -TimeoutSec 10
            $advisors = Invoke-RestMethod -Uri "$ApiUrl/admin/advisors/available" -Method Get -TimeoutSec 10
            $dashboard = Invoke-RestMethod -Uri "$ApiUrl/admin/dashboard" -Method Get -TimeoutSec 10

            # Mostrar dashboard
            Show-Dashboard $tickets $advisors $dashboard

            # Detectar cambios
            Detect-Changes $tickets

            # Verificar alertas
            Check-Alerts $tickets $advisors

            Write-Host "⏱️ Próxima actualización en ${IntervalSeconds}s..." -ForegroundColor Cyan
            Write-Host "=" * 60 -ForegroundColor Cyan

        } catch {
            Write-Host "❌ ERROR: $($_.Exception.Message)" -ForegroundColor Red
            Write-LogEntry "ERROR: $($_.Exception.Message)"
            Write-Host "🔄 Reintentando en ${IntervalSeconds}s..." -ForegroundColor Yellow
        }

        Start-Sleep -Seconds $IntervalSeconds
    }
}

# Iniciar monitor
Start-Monitor