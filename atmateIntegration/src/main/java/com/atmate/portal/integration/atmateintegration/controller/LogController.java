package com.atmate.portal.integration.atmateintegration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

@RestController
public class LogController {

    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    @Value("${log.path}")
    private String logFilePath;

    @GetMapping(value = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamLogs() {

        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(i -> Flux.fromStream(readLastLines()))
                .map(line -> {
                    return "data: " + line + "\n\n"; // Formato SSE
                });
    }

    private Stream<String> readLastLines() {
        String fullLogPath = logFilePath + "integration-api.log";

        try {
            List<String> allLines = Files.readAllLines(Paths.get(fullLogPath));

            int totalLines = allLines.size();
            int startLine = Math.max(0, totalLines - 10); // Últimas 10 linhas

            return allLines.subList(startLine, totalLines).stream();
        } catch (IOException e) {
            return Stream.of("Erro ao ler logs: " + e.getMessage());
        }
    }
}
