import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const client = new grpc.Client();
client.load(['/proto'], 'load.proto');

const grpcUnaryDuration = new Trend('grpc_unary_duration_ms');
const grpcUnaryErrors = new Counter('grpc_unary_errors_total');

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        grpc_unary_duration_ms: ['p(95) < 10000'],
        grpc_unary_errors_total: ['count == 0'],
    },
};

let connected = false;

export default function () {
    const target = __ENV.GRPC_TARGET || '104.155.69.110:9091';

    if (!connected) {
        client.connect(target, { plaintext: true });
        connected = true;
    }

    const params = {
        duration_sec: 10,
        msg_per_sec: 1,
        payload_bytes: 1024,
        jitter_ms: 0,
        fail_after_sec: 0,
    };

    const start = Date.now();
    const res = client.invoke('LoadService/Unary', params);
    grpcUnaryDuration.add(Date.now() - start);

    const ok = res && res.status === grpc.StatusOK;
    if (!ok) grpcUnaryErrors.add(1);

    check(res, { 'gRPC status OK': (r) => r && r.status === grpc.StatusOK });
    sleep(1);
}
