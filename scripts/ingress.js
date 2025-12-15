import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.TARGET || 'http://104.155.69.110:8080';
const SIZE_MB = Number(__ENV.SIZE_MB || 10);

const body = new Uint8Array(SIZE_MB * 1024 * 1024);

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<5000'],
    },
};

export default function () {
    const res = http.post(`${BASE}/api/ingress`, body, {
        headers: { 'Content-Type': 'application/octet-stream' },
        tags: { endpoint: 'ingress', sizeMB: String(SIZE_MB) },
    });
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(0.1);
}
