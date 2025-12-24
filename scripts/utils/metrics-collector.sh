#!/bin/bash

#=============================================================================
# COOPFILA - Metrics Collector
#=============================================================================
# Recolecta métricas del sistema durante pruebas de performance
# Usage: ./scripts/utils/metrics-collector.sh [duration_seconds] [output_file]
#=============================================================================

DURATION=${1:-60}
OUTPUT_FILE=${2:-"metrics-$(date +%Y%m%d-%H%M%S).csv"}

echo "timestamp,cpu_app,mem_app_mb,cpu_postgres,mem_postgres_mb,db_connections,tickets_waiting,tickets_completed,tickets_called,advisors_busy,advisors_available" > "$OUTPUT_FILE"

echo "📊 Collecting metrics for ${DURATION} seconds..."
echo "📁 Output: ${OUTPUT_FILE}"

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))

while [ $(date +%s) -lt $END_TIME ]; do
    TIMESTAMP=$(date +%Y-%m-%d\ %H:%M:%S)
    
    # Container stats
    APP_STATS=$(docker stats ticketero-app-dev --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" 2>/dev/null | head -1)
    APP_CPU=$(echo "$APP_STATS" | cut -d',' -f1 | tr -d '%')
    APP_MEM=$(echo "$APP_STATS" | cut -d',' -f2 | cut -d'/' -f1 | tr -d 'MiB ')
    
    PG_STATS=$(docker stats ticketero-postgres-dev --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" 2>/dev/null | head -1)
    PG_CPU=$(echo "$PG_STATS" | cut -d',' -f1 | tr -d '%')
    PG_MEM=$(echo "$PG_STATS" | cut -d',' -f2 | cut -d'/' -f1 | tr -d 'MiB ')
    
    # Database metrics
    DB_CONNECTIONS=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
        "SELECT count(*) FROM pg_stat_activity WHERE datname='ticketero';" 2>/dev/null | xargs)
    
    # Ticket stats
    TICKETS_WAITING=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='EN_ESPERA';" 2>/dev/null | xargs)
    TICKETS_COMPLETED=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='COMPLETADO';" 2>/dev/null | xargs)
    TICKETS_CALLED=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='ATENDIENDO';" 2>/dev/null | xargs)
    
    # Advisor stats
    ADVISORS_BUSY=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM advisor WHERE status='BUSY';" 2>/dev/null | xargs)
    ADVISORS_AVAILABLE=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM advisor WHERE status='AVAILABLE';" 2>/dev/null | xargs)
    
    # Write to CSV
    echo "${TIMESTAMP},${APP_CPU:-0},${APP_MEM:-0},${PG_CPU:-0},${PG_MEM:-0},${DB_CONNECTIONS:-0},${TICKETS_WAITING:-0},${TICKETS_COMPLETED:-0},${TICKETS_CALLED:-0},${ADVISORS_BUSY:-0},${ADVISORS_AVAILABLE:-0}" >> "$OUTPUT_FILE"
    
    sleep 5
done

echo "✅ Metrics collection complete: ${OUTPUT_FILE}"