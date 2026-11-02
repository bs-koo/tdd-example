package com.sqisoft.reservation.application.fake;

import java.util.concurrent.atomic.AtomicLong;

import com.sqisoft.reservation.application.TokenGenerator;

/**
 * 테스트 전용 {@link TokenGenerator} Fake 구현. 프로덕션 코드가 아니다.
 * 발급 순서를 예측할 수 있어야 "어떤 토큰이 무효화되었는가"를 단언할 수 있다.
 */
public class SequentialTokenGenerator implements TokenGenerator {

    private final AtomicLong sequence = new AtomicLong();

    @Override
    public String generate() {
        return "token-" + sequence.incrementAndGet();
    }
}
