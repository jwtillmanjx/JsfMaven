package com.jtiln.shared.commoncore.security.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EncryptionUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int IV_SIZE = 16;
    private static final int KEY_SIZE = 16;

    public static String encrypt(String plainText, String encryptKey) throws GeneralSecurityException {
        byte[] plainBytes = plainText.getBytes();

        // Generating IV.
        byte[] ivBytes = new byte[IV_SIZE];
        SECURE_RANDOM.nextBytes(ivBytes);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        SecretKeySpec secretKeySpec = aesSecretKey(encryptKey);

        // Encrypt.
        byte[] encrypted = applyEncryptionCipher(Cipher.ENCRYPT_MODE, plainBytes, secretKeySpec, ivParameterSpec);

        // Combine IV and encrypted part.
        byte[] encryptedIVAndText = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(ivBytes, 0, encryptedIVAndText, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, encryptedIVAndText, IV_SIZE, encrypted.length);

        return Base64.getEncoder().encodeToString(encryptedIVAndText);
    }

    public static String decrypt(String encryptedIvText, String decryptKey) throws GeneralSecurityException {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedIvText);

        // Extract IV.
        byte[] ivBytes = new byte[IV_SIZE];
        System.arraycopy(encryptedBytes, 0, ivBytes, 0, IV_SIZE);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        // Extract encrypted part.
        int encryptedSize = encryptedBytes.length - IV_SIZE;
        byte[] encrypted = new byte[encryptedSize];
        System.arraycopy(encryptedBytes, IV_SIZE, encrypted, 0, encryptedSize);

        SecretKeySpec secretKeySpec = aesSecretKey(decryptKey);

        // Decrypt.
        return new String(applyEncryptionCipher(Cipher.DECRYPT_MODE, encrypted, secretKeySpec, ivParameterSpec));
    }

    private static byte[] applyEncryptionCipher(int opmode, byte[] input, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        Cipher cipherDecrypt = Cipher.getInstance("AES/CTR/NoPadding");
        cipherDecrypt.init(opmode, secretKeySpec, ivParameterSpec);
        return cipherDecrypt.doFinal(input);
    }

    private static SecretKeySpec aesSecretKey(String secretKey) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = new byte[KEY_SIZE];
        System.arraycopy(messageDigest.digest(), 0, keyBytes, 0, keyBytes.length);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
