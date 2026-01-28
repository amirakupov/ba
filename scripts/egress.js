import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.TARGET || 'http://23.251.142.24:8080';
const SIZE = __ENV.SIZE || '5MB';
const CHUNK = __ENV.CHUNK || '65536';
const RUN_ID = __ENV.RUN_ID || 'gcp_e2e_003';

export const options = {
    vus: 10,
    duration: "3m",
    gracefulStop: "1m",
    discardResponseBodies: true,
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<5000"],
    },
};

export default function () {
    const url = `${BASE}/api/egress?size=${SIZE}&chunk=${CHUNK}`;
    const res = http.get(url, {
        headers: { "Accept-Encoding": "identity" },
        tags: { endpoint: "egress", size: SIZE, run_id: RUN_ID },
    });
    if (__VU === 1 && __ITER === 0) {
        console.log("status=", res.status, "CL=", res.headers["Content-Length"], "TE=", res.headers["Transfer-Encoding"]);
    }
    check(res, { "status is 200": (r) => r.status === 200 });
    sleep(0.1);
}
