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
        logger.info("Endpoint /logs foi chamado!");

        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(i -> Flux.fromStream(readLastLines()))
                .map(line -> {
                    logger.info("Enviando linha: {}", line);
                    return "data: " + line + "\n\n"; // Formato SSE
                });
    }

    private Stream<String> readLastLines() {
        String fullLogPath = logFilePath + "integration-api.log";

        if (!Files.exists(Paths.get(fullLogPath))) {
            logger.info("Ficheiro de logs não encontrado: {}", fullLogPath);
            return Stream.of("Erro: Ficheiro de logs não encontrado.");
        }

        try {
            logger.info("Lendo o ficheiro de logs: {}", fullLogPath);
            List<String> allLines = Files.readAllLines(Paths.get(fullLogPath));

            int totalLines = allLines.size();
            int startLine = Math.max(0, totalLines - 10); // Últimas 10 linhas

            logger.info("Total de linhas: {}, Enviando as últimas 10.", totalLines);
            return allLines.subList(startLine, totalLines).stream();
        } catch (IOException e) {
            logger.info("Falha ao ler logs: {}", e.getMessage());
            return Stream.of("Erro ao ler logs: " + e.getMessage());
        }
    }
}
