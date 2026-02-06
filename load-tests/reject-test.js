export default function () {
    const payload = JSON.stringify({
        clientId: 123,
        category: "urgent",
        theme: "Ночь",
        createdAt: "2026-02-06T03:00:00+05:00"  // 03:00 — вне 9–18
    });

    const res = http.post('http://localhost:8083/send', payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(1);
}