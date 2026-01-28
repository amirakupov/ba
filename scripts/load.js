import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 25, duration: '30s',
    thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<1500'] }
};

const BASE = 'http://18.102.18.62:8080';

export default function () {
    const res = http.get(`${BASE}/api/cpu?ms=200`);
    check(res, { 'status 200': r => r.status === 200 });
    sleep(0.1);
}
