import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';
import {Trend, Counter} from 'k6/metrics';

const client = new grpc.Client();
client.load(['/proto'], 'load.proto');

const grpcStreamDuration = new Trend('grpc_stream_duration_ms');
const grpcStreamErrors = new Counter('grpc_stream_errors_total');

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        grpc_stream_duration_ms: ['p(95) < 10000'],
        grpc_stream_errors_total: ['count == 0']
    },
}
export default function () {
    const target = __ENV.GRPC_TARGET || 'app:9091';
    client.connect(target, {
        plaintext:true,
    })
    const params = {
        duration_sec: 10,
        msg_per_sec: 5,
        payload_bytes: 1024,
        jitter_ms: 0,
        fail_after_sec: 0
    }
    const start = Date.now()
    const res = client.invoke('LoadService/StartStream', params)
    const elapsed = Date.now() - start;
    grpcStreamDuration.add(elapsed)
    console.log(`gRPC status=${res && res.status}, message=${JSON.stringify(res && res.message)}`);

    const ok = res && res.status === grpc.StatusOK;
    if (!ok) {
        grpcStreamErrors.add(1);
    }
    check(res, {
        'gRPC status OK': (r) => r && r.status === grpc.StatusOK,
    });
    client.close();
    sleep(1);
}