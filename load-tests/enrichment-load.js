import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 50,          // количество виртуальных пользователей
    duration: '30s',  // длительность теста
};

export default function () {
    const res = http.get('http://localhost:8082/extended-info/123');

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 100ms': (r) => r.timings.duration < 100,
    });

    sleep(1);
}
