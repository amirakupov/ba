import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 25, duration: '30s',
    thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(99)<1500'] }
};

const BASE = 'http://104.155.69.110:8080';

export default function () {
    const res = http.get(`${BASE}/api/cpu?ms=200`);
    check(res, { 'status 200': r => r.status === 200 });
    sleep(0.1);
}
