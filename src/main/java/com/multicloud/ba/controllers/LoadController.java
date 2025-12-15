package com.multicloud.ba.controllers;

import com.multicloud.ba.service.LoadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoadController {

    private final LoadService loadService;

    public LoadController(LoadService loadService) {
        this.loadService = loadService;
    }

    @GetMapping("/cpu")
    public Map<String,Object> cpu(
            @RequestParam(defaultValue = "200") @Min(1) @Max(60_000) int ms) {
        return loadService.loadCpu(ms);
    }

    @GetMapping("/egress")
    public void egress(@RequestParam("size") String sizeParam,
                       @RequestParam(name = "chunk", defaultValue = "65536") int chunk,
                       HttpServletResponse response) throws IOException {

        long sizeBytes = loadService.parseSize(sizeParam);
        loadService.validateSize(sizeBytes);

        if (chunk <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chunk must be > 0");

        loadService.recordEgress(sizeBytes);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(sizeBytes);

        byte[] buffer = new byte[chunk];

        long remaining = sizeBytes;
        try (OutputStream os = response.getOutputStream()) {
            while (remaining > 0) {
                int toWrite = (int) Math.min(buffer.length, remaining);
                os.write(buffer, 0, toWrite);
                remaining -= toWrite;
            }
            os.flush();
        }
    }

    @PostMapping("/ingress")
    public Map<String, Object> ingress(HttpServletRequest request) throws IOException {
        long total = 0;
        byte[] buf = new byte[64 * 1024];
        try (InputStream is = request.getInputStream()) {
            int n;
            while ((n = is.read(buf)) != -1) total += n;
        }
        loadService.recordIngress(total);

        return Map.of("ok", true, "bytes", total);
    }

    @PostMapping("/mixed")
    public void mixed(@RequestParam(defaultValue="200") int cpuMs,
                      @RequestParam(defaultValue="1MB") String egressSize,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {

        long inBytes = 0;
        byte[] buf = new byte[64*1024];
        try (InputStream is = request.getInputStream()) {
            int n;
            while ((n = is.read(buf)) != -1) inBytes += n;
        }
        loadService.recordIngress(inBytes);

        loadService.loadCpu(cpuMs);

        long outBytes = loadService.parseSize(egressSize);
        loadService.validateSize(outBytes);
        loadService.recordEgress(outBytes);

        response.setStatus(200);
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(outBytes);

        byte[] outBuf = new byte[64*1024];
        long rem = outBytes;
        try (OutputStream os = response.getOutputStream()) {
            while (rem > 0) {
                int w = (int) Math.min(outBuf.length, rem);
                os.write(outBuf, 0, w);
                rem -= w;
            }
            os.flush();
        }
    }
}
