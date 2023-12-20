package com.grameenphone.wipro.task_executor.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Formatter;

public class CryptoUtil {
    public static X509Certificate loadCertificate(String filePath) throws CertificateException, FileNotFoundException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
        return (X509Certificate)certificateFactory.generateCertificate(new FileInputStream(filePath));
    }

    public static PrivateKey loadPrivateKey(String plainText) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        plainText = plainText.replaceAll("\\s|(-+[^-]+-+)", "");
        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(plainText));
        return kf.generatePrivate(keySpecPKCS8);
    }

    public static PublicKey loadPublicKey(String plainText) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        plainText = plainText.replaceAll("\\s|(-+[^-]+-+)", "");
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(plainText));
        return kf.generatePublic(keySpecX509);
    }

    public static String encrypt(String algorithm, String src, String keyType, String key, String iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), keyType);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv.getBytes()));
        return Base64.getEncoder().encodeToString(cipher.doFinal(src.getBytes()));
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString().toUpperCase();
    }

    public static String macHash(String algorithm, String src, String base64Key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(base64Key.getBytes(), algorithm);
        mac.init(secretKeySpec);
        return toHexString(mac.doFinal(src.getBytes()));
    }

    public static String decrypt(String algorithm, String src, String keyType, String key, String iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), keyType);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv.getBytes()));
        return new String(cipher.doFinal(Base64.getDecoder().decode(src)));
    }

    public static String decrypt(String algorithm, String src, PrivateKey key, String iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv == null ? null : new IvParameterSpec(iv.getBytes()));
        return new String(cipher.doFinal(Base64.getDecoder().decode(src)));
    }

    public static String sign(String algorithm, String plainText, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(privateKey);
        signature.update(plainText.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean verify(String algorithm, String plainText, String hash, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(algorithm);
        signature.initVerify(publicKey);
        signature.update(plainText.getBytes());
        return signature.verify(Base64.getDecoder().decode(hash));
    }
}
