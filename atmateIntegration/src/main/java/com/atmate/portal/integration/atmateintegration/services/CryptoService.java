package com.atmate.portal.integration.atmateintegration.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class CryptoService {
    private static final int GCM_TAG_LENGTH = 128; // bits

    @Autowired
    private KeyService keyService;

    public String decrypt(String cipherText) throws Exception {
        SecretKey secretKey = keyService.loadKey();
        byte[] decodedCipherText = Base64.getDecoder().decode(cipherText);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, decodedCipherText, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] decryptedText = cipher.doFinal(decodedCipherText, 12, decodedCipherText.length - 12);
        return new String(decryptedText, StandardCharsets.UTF_8);
    }
}