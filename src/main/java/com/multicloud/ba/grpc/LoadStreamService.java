package com.multicloud.ba.grpc;

import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.devh.boot.grpc.server.service.GrpcService;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Instant;
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
    public void startStream(com.multicloud.ba.grpc.LoadParams request, StreamObserver<com.multicloud.ba.grpc.StreamEvent> responseObserver) {

        streamsStarted.increment();

        long startNanos = System.nanoTime();

        int durationSec  = request.getDurationSec();
        double msgPerSec = request.getMsgPerSec();
        int payloadBytes = request.getPayloadBytes();
        int failAfterSec = request.getFailAfterSec();

        if (durationSec <= 0 || msgPerSec <= 0 || payloadBytes <= 0) {
            responseObserver.onError(
                    new IllegalArgumentException("Invalid params")
            );
            return;
        }

        long totalMessages = (long) (durationSec * msgPerSec);
        long intervalMs = (long) (1000.0 / msgPerSec);

        byte[] payload = new byte[payloadBytes];

        try {
            for (long i = 0; i < totalMessages; i++) {

                long nowMs = Instant.now().toEpochMilli();

                long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
                double cpuLoad = getProcessCpuLoadSafe();

                com.multicloud.ba.grpc.StreamEvent event = com.multicloud.ba.grpc.StreamEvent.newBuilder()
                        .setSeq(i)
                        .setTsMs(nowMs)
                        .setPayload(com.google.protobuf.ByteString.copyFrom(payload))
                        .setCpuLoad(cpuLoad)
                        .setHeapUsed(heapUsed)
                        .build();

                responseObserver.onNext(event);

                messagesSent.increment();
                grpcPayloadBytes.increment(payloadBytes);

                if (failAfterSec > 0 && (i * intervalMs) / 1000 >= failAfterSec) {
                    throw new RuntimeException("Fail after " + failAfterSec + " seconds (test)");
                }

                Thread.sleep(intervalMs);
            }

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
