import { Client, Stream } from 'k6/net/grpc';
import { sleep, check } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const client = new Client();
client.load(['/proto'], 'load.proto');

const streamDurationMs = new Trend('grpc_stream_duration_ms');
const streamErrorsTotal = new Counter('grpc_stream_errors_total');
const streamMessagesTotal = new Counter('grpc_stream_messages_total');
const streamPayloadBytesTotal = new Counter('grpc_stream_payload_bytes_total');

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        grpc_stream_errors_total: ['count == 0'],
        grpc_stream_duration_ms: ['p(95) < 15000'], // под duration_sec=10 это разумно
    },
};

export default function () {
    const target = __ENV.GRPC_TARGET || '104.155.69.110:9091';
    
    if (__ITER === 0) {
        client.connect(target, { plaintext: true });
    }

    const params = {
        duration_sec: 10,
        msg_per_sec: 5,
        payload_bytes: 1024,
        jitter_ms: 0,
        fail_after_sec: 0,
    };

    const start = Date.now();
    let msgCount = 0;
    let byteCount = 0;
    let done = false;
    let hadError = false;

    const stream = new Stream(client, 'LoadService/Stream', {
        tags: { rpc: 'Stream' },
    });

    stream.on('data', (ev) => {
        msgCount++;

        byteCount += params.payload_bytes;
    });

    stream.on('error', (e) => {
        hadError = true;
        streamErrorsTotal.add(1);
        done = true;
    });

    stream.on('end', () => {
        streamDurationMs.add(Date.now() - start);
        streamMessagesTotal.add(msgCount);
        streamPayloadBytesTotal.add(byteCount);
        done = true;
    });

    stream.write(params);
    const timeoutMs = params.duration_sec * 1000 + 5000;
    while (!done && (Date.now() - start) < timeoutMs) {
        sleep(0.01);
    }

    if (!done) {
        streamErrorsTotal.add(1);
        hadError = true;
    }

    check({ hadError }, { 'stream finished without error': (x) => !x.hadError });

    sleep(1);
}
