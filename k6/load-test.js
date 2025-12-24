// =============================================================================
// COOPFILA - K6 Load Test Base
// =============================================================================
// Usage: k6 run --vus 10 --duration 2m k6/load-test.js
// =============================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const ticketsCreated = new Counter('tickets_created');
const ticketErrors = new Rate('ticket_errors');
const createLatency = new Trend('create_latency', true);

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const QUEUES = ['CAJA', 'PERSONAL', 'EMPRESAS', 'GERENCIA'];

// Test options (can be overridden via CLI)
export const options = {
    vus: 10,
    duration: '2m',
    thresholds: {
        http_req_duration: ['p(95)<2000'], // p95 < 2s
        ticket_errors: ['rate<0.01'], // < 1% errors
        tickets_created: ['count>50'], // > 50 tickets
    },
};

// Unique ID generator
function generateNationalId() {
    return Math.floor(10000000 + Math.random() * 90000000).toString();
}

function generatePhone() {
    return '+569' + Math.floor(10000000 + Math.random() * 90000000);
}

// Main test function
export default function () {
    const queue = QUEUES[Math.floor(Math.random() * QUEUES.length)];
    
    const payload = JSON.stringify({
        nationalId: generateNationalId(),
        telefono: generatePhone(),
        branchOffice: 'Sucursal Centro',
        queueType: queue,
    });
    
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: { name: 'CreateTicket' },
    };
    
    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/tickets`, payload, params);
    const duration = Date.now() - startTime;
    
    // Record metrics
    createLatency.add(duration);
    
    const success = check(response, {
        'status is 201': (r) => r.status === 201,
        'has ticket number': (r) => r.json('numero') !== undefined,
        'has position': (r) => r.json('positionInQueue') > 0,
    });
    
    if (success) {
        ticketsCreated.add(1);
    } else {
        ticketErrors.add(1);
        console.log(`Error: ${response.status} - ${response.body}`);
    }
    
    // Think time between requests
    sleep(Math.random() * 2 + 1); // 1-3 seconds
}

// Summary handler
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'results/load-test-summary.json': JSON.stringify(data, null, 2),
    };
}

function textSummary(data, options) {
    const checks = data.metrics.checks;
    const duration = data.metrics.http_req_duration;
    
    return `
═══════════════════════════════════════════════════════════════
COOPFILA - LOAD TEST RESULTS
═══════════════════════════════════════════════════════════════

Total Requests: ${data.metrics.http_reqs.values.count}
Tickets Created: ${data.metrics.tickets_created?.values.count || 0}
Error Rate: ${(data.metrics.ticket_errors?.values.rate * 100 || 0).toFixed(2)}%

Latency:
  p50: ${duration.values['p(50)'].toFixed(0)}ms
  p95: ${duration.values['p(95)'].toFixed(0)}ms
  p99: ${duration.values['p(99)'].toFixed(0)}ms
  max: ${duration.values.max.toFixed(0)}ms

Throughput: ${(data.metrics.http_reqs.values.count / (data.state.testRunDurationMs / 1000 / 60)).toFixed(1)} req/min

═══════════════════════════════════════════════════════════════
`;
}