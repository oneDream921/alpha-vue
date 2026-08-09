package io.github.onedream921.alphavue.modules.system.settings.config;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Encrypts database-managed credentials. The master key remains deployment-owned. */
@Component
public class SettingCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKey key;

    public SettingCipher(@Value("${alpha.system-settings.master-key:}") String masterKey) {
        if (masterKey == null || !masterKey.matches("[A-Fa-f0-9]{64}")) {
            throw new IllegalStateException("SYSTEM_SETTINGS_MASTER_KEY must be a 64-character hexadecimal key");
        }
        byte[] bytes = java.util.HexFormat.of().parseHex(masterKey);
        key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt system setting", exception);
        }
    }

    public String decrypt(String value) {
        try {
            String[] parts = value.split("\\.", -1);
            if (parts.length != 2) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128,
                    Base64.getUrlDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(parts[1])), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BusinessException(500, PublicErrorMessage.INTERNAL_SERVER_ERROR);
        }
    }
}
