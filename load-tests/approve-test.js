import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 3 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<400'],
        http_req_failed: ['rate<0.01'],
        checks: ['rate>0.98'],
    },
};

export default function () {
    // Уникальный eventId
    const eventId = crypto.randomUUID();

    // Время внутри рабочего дня (10:00–17:00 +05:00), без миллисекунд
    const hour = Math.floor(Math.random() * 7) + 10; // 10–16
    const minute = Math.floor(Math.random() * 60);
    const createdAt = `2026-02-07T${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:00+05:00`;

    // Категории, которые почти всегда проходят
    const categories = ['support', 'complaint']; // ← только не-restricted, чтобы 100% APPROVED
    const category = categories[Math.floor(Math.random() * categories.length)];

    // Случайный clientId
    const clientId = Math.floor(Math.random() * 9900) + 100;

    const payload = JSON.stringify({
        eventId: eventId,
        clientId: clientId,
        category: category,
        theme: `Approved test ${category} at ${createdAt}`,
        createdAt: createdAt,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post('http://localhost:8083/send', payload, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 300ms': (r) => r.timings.duration < 300,
        'body contains "Event sent"': (r) => r.body.includes('Event sent successfully'),
    });

    sleep(Math.random() * 1.2 + 0.3); // 0.3–1.5 секунды
}