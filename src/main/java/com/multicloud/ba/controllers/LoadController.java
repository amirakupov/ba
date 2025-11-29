package com.multicloud.ba.controllers;

import com.multicloud.ba.service.LoadService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
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

        if (chunk <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chunk must be > 0");
        }

        loadService.recordEgress(sizeBytes);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(sizeBytes);

        byte[] buffer = new byte[chunk];
        long remaining = sizeBytes;

        SecureRandom secureRandom = new SecureRandom();

        try (OutputStream os = response.getOutputStream()) {
            while (remaining > 0) {
                int toWrite = (int) Math.min(buffer.length, remaining);

                secureRandom.nextBytes(buffer);

                os.write(buffer, 0, toWrite);
                os.flush();
                remaining -= toWrite;
            }
        }
    }
    @PostMapping("/ingress")
    public Map<String, Object> ingress(@RequestBody byte[] body) {
        int len = body != null ? body.length : 0;

        loadService.recordIngress(len);

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("bytes", len);
        return resp;
    }



}
