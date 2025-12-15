package com.multicloud.ba.grpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import java.util.concurrent.TimeUnit;

@GrpcService
public class LoadStreamService extends LoadServiceGrpc.LoadServiceImplBase{

    private final Counter streamsStarted;
    private final Counter messagesSent;
    private final Counter grpcPayloadBytes;
    private final Timer streamDuration;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    public LoadStreamService(MeterRegistry registry) {
        this.streamsStarted = Counter.builder("ba_grpc_streams_started_total").register(registry);
        this.messagesSent = Counter.builder("ba_grpc_messages_sent_total").register(registry);
        this.grpcPayloadBytes = Counter.builder("ba_grpc_payload_bytes_total").register(registry);
        this.streamDuration = Timer.builder("ba_grpc_stream_duration_seconds").register(registry);
    }

    @Override
    public void stream(LoadParams req, StreamObserver<StreamEvent> out) {
        streamsStarted.increment();
        long startNs = System.nanoTime();

        int durationSec = req.getDurationSec();
        int payloadBytes = req.getPayloadBytes();
        double mps = req.getMsgPerSec();
        int jitterMs = req.getJitterMs();
        int failAfter = req.getFailAfterSec();

        if (durationSec <= 0 || mps <= 0 || payloadBytes <= 0) {
            out.onError(new IllegalArgumentException("Invalid params"));
            return;
        }
        final ServerCallStreamObserver<StreamEvent> serverObs =
                (out instanceof ServerCallStreamObserver) ? (ServerCallStreamObserver<StreamEvent>) out : null;

        byte[] payload = new byte[payloadBytes];
        ByteString payloadBs = ByteString.copyFrom(payload);

        long endNs = startNs + durationSec * 1_000_000_000L;
        long failAtNs = (failAfter > 0) ? (startNs + failAfter * 1_000_000_000L) : Long.MAX_VALUE;

        long periodNs = Math.max(1L, (long) (1_000_000_000.0 / mps));
        long nextSendNs = System.nanoTime();

        long seq = 0;
        try {
            while (System.nanoTime() < endNs) {

                if (serverObs != null && serverObs.isCancelled()) break;
                if (System.nanoTime() >= failAtNs) throw new RuntimeException("Injected failure (fail_after_sec)");

                // wait until next slot
                long now = System.nanoTime();
                if (now < nextSendNs) LockSupport.parkNanos(nextSendNs - now);
                nextSendNs += periodNs;

                if (jitterMs > 0) {
                    long jitterNs = (long) (ThreadLocalRandom.current().nextInt(-jitterMs, jitterMs + 1)) * 1_000_000L;
                    nextSendNs = Math.max(nextSendNs + jitterNs, System.nanoTime());
                }

                seq++;
                StreamEvent ev = StreamEvent.newBuilder()
                        .setSeq(seq)
                        .setTsMs(System.currentTimeMillis())
                        .setPayload(payloadBs)
                        .setCpuLoad(getProcessCpuLoadSafe())
                        .setHeapUsed(memoryMXBean.getHeapMemoryUsage().getUsed())
                        .build();

                out.onNext(ev);
                messagesSent.increment();
                grpcPayloadBytes.increment(payloadBytes);
            }

            streamDuration.record(System.nanoTime() - startNs, TimeUnit.NANOSECONDS);
            out.onCompleted();
        } catch (Exception e) {
            streamDuration.record(System.nanoTime() - startNs, TimeUnit.NANOSECONDS);
            out.onError(e);
        }
    }

    @Override
    public void unary(LoadParams request, StreamObserver<StreamEvent> responseObserver) {

        streamsStarted.increment();
        long startNanos = System.nanoTime();

        int durationSec  = request.getDurationSec();
        int payloadBytes = request.getPayloadBytes();

        if (durationSec <= 0 || payloadBytes <= 0) {
            responseObserver.onError(new IllegalArgumentException("Invalid params"));
            return;
        }

        byte[] payload = new byte[payloadBytes];
        ByteString payloadBs = ByteString.copyFrom(payload);

        try {
            long end = System.nanoTime() + durationSec * 1_000_000_000L;
            long i = 0;
            while (System.nanoTime() < end) {
                i++;
            }

            long nowMs   = System.currentTimeMillis();
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            double cpuLoad = getProcessCpuLoadSafe();

            StreamEvent event = StreamEvent.newBuilder()
                    .setSeq(i)
                    .setTsMs(nowMs)
                    .setPayload(payloadBs)
                    .setCpuLoad(cpuLoad)
                    .setHeapUsed(heapUsed)
                    .build();

            messagesSent.increment();
            grpcPayloadBytes.increment(payloadBytes);

            responseObserver.onNext(event);
            long durationNanos = System.nanoTime() - startNanos;
            streamDuration.record(durationNanos, TimeUnit.NANOSECONDS);

            responseObserver.onCompleted();

        } catch (Exception e) {
            long durationNanos = System.nanoTime() - startNanos;
            streamDuration.record(durationNanos, TimeUnit.NANOSECONDS);
            responseObserver.onError(e);
        }
    }


    private double getProcessCpuLoadSafe() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return osBean.getProcessCpuLoad();
        } catch (Throwable t) {
            return -1.0;
        }
    }
}
