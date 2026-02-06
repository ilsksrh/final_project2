import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    scenarios: {

        producer_api: {
            executor: 'ramping-vus',
            exec: 'sendEvent',
            stages: [
                { duration: '10s', target: 20 },
                { duration: '30s', target: 60 }, // ↓ было 80
                { duration: '20s', target: 60 },
                { duration: '10s', target: 0 },
            ],
        },

        enrichment_api: {
            executor: 'constant-arrival-rate',
            exec: 'getEnrichment',
            rate: 40,           // ↓ было 50
            timeUnit: '1s',
            duration: '40s',
            preAllocatedVUs: 10,
            maxVUs: 20,         // ↓ было 30
        },

        duplicates: {
            executor: 'constant-vus',
            exec: 'sendDuplicateEvent',
            vus: 10,
            duration: '30s',
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.05'],
        checks: ['rate>0.95'],
    },
};


// ==========================
// SCENARIO IMPLEMENTATIONS
// ==========================

export function sendEvent() {
    const now = new Date().toISOString().replace(/\.\d{3}/, '');

    const payload = JSON.stringify({
        clientId: Math.floor(Math.random() * 1000),
        category: ['urgent', 'credit', 'loan', 'support'][Math.floor(Math.random() * 4)],
        theme: 'Load test event',
        createdAt: now,
    });

    const res = http.post('http://localhost:8083/send', payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, {
        'producer status 200': (r) => r.status === 200,
    });

    sleep(0.3);
}

export function getEnrichment() {
    const clientId = Math.floor(Math.random() * 1000);

    const res = http.get(`http://localhost:8082/extended-info/${clientId}`);

    check(res, {
        'enrichment status 200': (r) => r.status === 200,
    });
}

const duplicateEventId = uuidv4();

export function sendDuplicateEvent() {
    const payload = JSON.stringify({
        eventId: duplicateEventId,
        clientId: 999,
        category: 'urgent',
        theme: 'Duplicate event test',
        createdAt: new Date().toISOString().replace(/\.\d{3}/, ''),
    });

    const res = http.post('http://localhost:8083/send', payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, {
        'duplicate accepted by producer': (r) => r.status === 200,
    });

    sleep(0.5);
}
