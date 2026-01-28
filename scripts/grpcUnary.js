import grpc from 'k6/net/grpc';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const client = new grpc.Client();
client.load(['/proto'], 'load.proto');

const unary_roundtrip_ms = new Trend('unary_roundtrip_ms', true);
const unary_errors_total = new Counter('unary_errors_total');

export const options = {
    scenarios: {
        unary: {
            executor: 'constant-arrival-rate',
            rate: Number( 20),
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 10,
            maxVUs: 50,
        },
    },
    thresholds: {
        unary_roundtrip_ms: ['p(95)<2000'],
        unary_errors_total: ['count==0'],
        grpc_req_duration: ['p(95)<2000'],
    },
};

let connected = false;

export default function () {
    const target = __ENV.GRPC_TARGET;
    if (!connected) {
        client.connect(target, { plaintext: true });
        connected = true;
    }

    const params = {
        duration_sec: Number(__ENV.DURATION_SEC || 1),
        msg_per_sec: 1,
        payload_bytes: Number(__ENV.PAYLOAD_BYTES || 1024),
        jitter_ms: 0,
        fail_after_sec: 0,
    };

    const t0 = Date.now();
    const res = client.invoke('LoadService/Unary', params);
    unary_roundtrip_ms.add(Date.now() - t0);

    const ok = res && res.status === grpc.StatusOK;
    if (!ok) unary_errors_total.add(1);

    check(res, { 'gRPC status OK': (r) => r && r.status === grpc.StatusOK });
}
