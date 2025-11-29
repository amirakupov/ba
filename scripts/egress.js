import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.TARGET || 'http://app:8080';
const SIZE = __ENV.SIZE || '10MB';
const CHUNK = __ENV.CHUNK || '65536';

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<5000'],
    },
};

export default function () {
    const url = `${BASE}/api/egress?size=${SIZE}&chunk=${CHUNK}`;

    const res = http.get(url, {
        responseType: 'none',
        tags: { endpoint: 'egress', size: SIZE },
    });

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(0.1);
}
