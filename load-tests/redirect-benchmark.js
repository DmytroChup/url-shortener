import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<30', 'p(99)<45'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://host.docker.internal:8081';

export function setup() {
    const shortCodes = [];
    for (let i = 0; i < 10; i++) {
        const payload = JSON.stringify({
            url: `https://example.com/page-${i}`,
            ttlDays: 30,
        });
        const res = http.post(`${BASE_URL}/api/v1/urls/shorten`, payload, {
            headers: { 'Content-Type': 'application/json' },
        });
        if (res.status === 200) {
            shortCodes.push(res.body.replace(/"/g, ''));
        }
    }
    return { shortCodes };
}

export default function (data) {
    const code = data.shortCodes[Math.floor(Math.random() * data.shortCodes.length)];

    const res = http.get(`${BASE_URL}/api/v1/urls/${code}`, {
        redirects: 0,
    });

    check(res, {
        'is status 302': (r) => r.status === 302,
    });
}