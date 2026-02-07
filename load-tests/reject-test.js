import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 5 },
        { duration: '30s', target: 5 },
        { duration: '10s', target: 0 },
    ],
};

export default function () {
    http.post(
        'http://localhost:8083/events',
        JSON.stringify({
            clientId: Math.floor(Math.random() * 10000),
            category: 'support',
            theme: 'k6 load test',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    sleep(1);
}
