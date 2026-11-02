package com.sqisoft.reservation.infra;

import java.security.SecureRandom;
import java.util.Base64;

import com.sqisoft.reservation.application.TokenGenerator;

/**
 * 256비트 난수를 URL 안전 Base64로 인코딩한 불투명 토큰을 발급한다.
 * 토큰 자체에 아무 의미도 담지 않으므로 위조로 권한을 만들어낼 수 없다.
 */
public class SecureRandomTokenGenerator implements TokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
