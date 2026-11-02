package com.sqisoft.reservation.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.sqisoft.reservation.application.PasswordHasher;

/**
 * PBKDF2-HMAC-SHA256 기반 비밀번호 해시. 별도 의존성 없이 JDK 암호 API만 쓴다.
 * 저장 형식은 {@code 반복횟수:솔트(Base64):해시(Base64)} 이며, 반복 횟수를 함께 저장하므로
 * 나중에 반복 횟수를 올려도 기존 해시를 그대로 검증할 수 있다.
 */
public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final String SEPARATOR = ":";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] derived = derive(rawPassword, salt, ITERATIONS);
        return ITERATIONS + SEPARATOR + encode(salt) + SEPARATOR + encode(derived);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split(SEPARATOR);
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            // 길이가 같을 때 바이트별 비교 시간이 일정한 비교 — 타이밍 차이로 해시를 되짚지 못하게 한다.
            return MessageDigest.isEqual(expected, derive(rawPassword, salt, iterations));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] derive(String rawPassword, byte[] salt, int iterations) {
        // PBKDF2 구현이 빈 비밀번호를 거부하므로, 먼저 SHA-256으로 접어 길이가 항상 같은 입력으로 만든다.
        // 부수적으로 매우 긴 비밀번호도 같은 비용으로 처리된다.
        char[] folded = encode(sha256(rawPassword)).toCharArray();
        PBEKeySpec spec = new PBEKeySpec(folded, salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("비밀번호 해시에 실패했습니다.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
