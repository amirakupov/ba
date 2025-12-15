package com.multicloud.ba.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class LoadService {
    private final SecureRandom rnd = new SecureRandom();

    private final long maxBytes;

    private final Counter egressCounter;
    private final Counter ingressCounter;

    public LoadService(@Value("${EGRESS_MAX_MB:100}") long maxMb, MeterRegistry meterRegistry) {
        this.maxBytes = maxMb * 1024L * 1024L;
        this.egressCounter = Counter.builder("ba_egress_bytes_total").tag("endpoint", "api/egress").register(meterRegistry);
        this.ingressCounter = Counter.builder("ba_ingress_bytes_total").tag("endpoint", "api/ingress").register(meterRegistry);
    }

    public void recordIngress(long bytes) {
        if (bytes > 0) {
            ingressCounter.increment(bytes);
        }
    }

    public void recordEgress(long bytes) {
        if (bytes > 0) {
            egressCounter.increment(bytes);
        }
    }

    @Timed(value = "ba.cpu", extraTags = {"kind","burn"})
    public Map<String, Object> loadCpu(int ms) {
        long end = System.nanoTime() + ms * 1_000_000L;
        double x = 0;
        while (System.nanoTime() < end) {
            x += Math.sqrt(1234.5678 + (x % 1.0));
        }
        return map("ok", true, "ms", ms, "acc", x, "ts", System.currentTimeMillis());
    }

    private static Map<String,Object> map(Object... kv) {
        Map<String,Object> m = new HashMap<>();
        for (int i=0;i<kv.length;i+=2) m.put(String.valueOf(kv[i]), kv[i+1]);
        return m;
    }

    public long parseSize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size is required");
        }

        String s = raw.trim().toUpperCase(Locale.ROOT);
        long multiplier = 1L;

        if (s.endsWith("KB")) {
            multiplier = 1024L;
            s = s.substring(0, s.length() - 2);
        } else if (s.endsWith("MB")) {
            multiplier = 1024L * 1024L;
            s = s.substring(0, s.length() - 2);
        } else if (s.endsWith("GB")) {
            multiplier = 1024L * 1024L * 1024L;
            s = s.substring(0, s.length() - 2);
        } else if (s.endsWith("K")) {
            multiplier = 1024L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("M")) {
            multiplier = 1024L * 1024L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("G")) {
            multiplier = 1024L * 1024L * 1024L;
            s = s.substring(0, s.length() - 1);
        }

        long value;
        try {
            value = Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid size: " + raw);
        }

        if (value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be > 0");
        }

        long bytes = value * multiplier;
        if (bytes < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size is too big");
        }

        return bytes;
    }

    public void validateSize(long size) {
        if (size < 1 || size > maxBytes) {
            long maxMb = maxBytes / 1024L / 1024L;
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size must be between 1 and " + maxBytes + " bytes (EGRESS_MAX_MB=" + maxMb + "MB)"
            );
        }
    }


}
