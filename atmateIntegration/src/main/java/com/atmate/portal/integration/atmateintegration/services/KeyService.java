package com.atmate.portal.integration.atmateintegration.services;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Service
public class KeyService {
    @Value("${secretkey.name}")
    private String KEY_FILE;
    @Value("${secretkey.path}")
    private String KEY_PATH;
    public SecretKey loadKey() {
        try {
            String fullPath = KEY_PATH + KEY_FILE;
            byte[] encodedKey = Files.readAllBytes(Paths.get(fullPath));
            byte[] decodedKey = Base64.decodeBase64(encodedKey);
            return new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar a chave", e);
        }

    }
}
