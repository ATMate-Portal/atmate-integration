package com.atmate.portal.integration.atmateintegration.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
public class LogController {

    @Value("${log.path}")
    private String logFilePath;

    private long lastFileSize = 0;

    @GetMapping(value = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamLogs() {
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(i -> Flux.fromIterable(readNewLines()))
                .map(line -> line + "\n\n"); // Formato SSE correto
    }

    private List<String> readNewLines() {
        String fullLogPath = logFilePath + "integration-api.log";
        List<String> newLines = new ArrayList<>();

        if (!Files.exists(Paths.get(fullLogPath))) {
            return List.of("data: Erro: Ficheiro de logs não encontrado.\n\n");
        }

        try (RandomAccessFile file = new RandomAccessFile(fullLogPath, "r")) {
            long fileLength = file.length();

            if (fileLength < lastFileSize) {
                lastFileSize = 0;
            }

            file.seek(lastFileSize);

            String line;
            while ((line = file.readLine()) != null) {
                newLines.add(line + "\n"); // Adiciona quebra de linha corretamente
            }

            lastFileSize = file.getFilePointer();

            return newLines.isEmpty() ? List.of() : newLines;
        } catch (IOException e) {
            return List.of("data: Erro ao ler logs: " + e.getMessage() + "\n\n");
        }
    }
}
