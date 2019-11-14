package com.android.server.mqttservice;

import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    private final static String DEFAULT_SEED = "rgk_translator";

    //这种方式在6.0以上的版本不能用，如果是6.0以上版本请使用encrypt(String content, String password)方法
    private static SecretKey generateKey(String seed) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "Crypto");
        secureRandom.setSeed(seed.getBytes("UTF-8"));
        keyGenerator.init(128, secureRandom);
        return keyGenerator.generateKey();
    }

    //每次产生的密钥不一样
    private static SecretKey generateKey2(String seed) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.setSeed(seed.getBytes("UTF-8"));
        keyGenerator.init(128, secureRandom);
        return keyGenerator.generateKey();
    }

    private static byte[] encrypt(String content, SecretKey secretKey) throws Exception {
        byte[] enCodeFormat = secretKey.getEncoded();
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(content.getBytes("UTF-8"));
    }

    private static byte[] decrypt(byte[] content, SecretKey secretKey) throws Exception {
        byte[] enCodeFormat = secretKey.getEncoded();
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(content);
    }

    private static byte[] encrypt(String content, String password) throws Exception {
        SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES/CBC/PKCS5PADDING");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(content.getBytes("UTF-8"));
    }

    private static byte[] decrypt(byte[] content, String password) throws Exception {
        SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES/CBC/PKCS5PADDING");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(content);
    }

    private static SecretKey getDefaultKey() throws Exception {
        SecretKey secretKey = generateKey(DEFAULT_SEED);
        return secretKey;
    }

    public static String encrypt(String text) throws Exception {
            SecretKey secretKey = getDefaultKey();
            byte[] result = encrypt(text, secretKey);
            return new String(Base64.encode(result, Base64.DEFAULT),"UTF-8");
    }

    public static String decrypt(String text) throws Exception {
            SecretKey secretKey = getDefaultKey();
            byte[] tmpt = Base64.decode(text.getBytes("UTF-8"), Base64.DEFAULT);
            byte[] result = decrypt(tmpt, secretKey);
            return new String(result, "UTF-8");
    }
}
