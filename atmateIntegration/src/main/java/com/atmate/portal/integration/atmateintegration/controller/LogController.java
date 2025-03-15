package com.atmate.portal.integration.atmateintegration.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.util.stream.Stream;

@RestController
public class LogController {

    @Value("${log.path}")
    private String LOG_FILE_PATH;

    @GetMapping(value = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamLogs() {
        return Flux.interval(Duration.ofSeconds(1))
                   .flatMap(i -> Flux.fromStream(readLastLines()))
                   .map(line -> "data: " + line + "\n\n"); // Formato SSE
    }

    private Stream<String> readLastLines() {
        try {
            File file = new File(LOG_FILE_PATH + "integration-api.log");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return reader.lines().skip(Math.max(0, file.length() - 10)).onClose(() -> {
                try { reader.close(); } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            return Stream.of("Erro ao ler logs: " + e.getMessage());
        }
    }
}
