package com.atmate.portal.integration.atmateintegration.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogManager {
    private static final Logger logger = LoggerFactory.getLogger(LogManager.class);
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    static {
        new Thread(() -> {
            while (true) {
                try {
                    String logMessage = logQueue.take(); // Bloqueia até que haja um log
                    logger.info(logMessage); // Registra o log
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // Encerra o loop se a thread for interrompida
                }
            }
        }).start();
    }

    public static void log(String message) {
        logQueue.offer(message); // Adiciona a mensagem à fila
    }
}

