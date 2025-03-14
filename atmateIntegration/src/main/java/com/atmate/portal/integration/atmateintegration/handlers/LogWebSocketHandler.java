package com.atmate.portal.integration.atmateintegration.handlers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    @Value("${log.path}")
    private String logFilePath;

    private WebSocketSession session;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        executor.scheduleAtFixedRate(this::sendLogs, 0, 1, TimeUnit.SECONDS);
    }

    private void sendLogs() {
        if (session != null && session.isOpen()) {
            try {
                String logs = new String(Files.readAllBytes(Paths.get(logFilePath)));
                session.sendMessage(new TextMessage(logs));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}