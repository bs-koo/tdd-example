package com.sqisoft.reservation.domain;

import java.util.regex.Pattern;

public class MemberPolicy {

    static final int NAME_MAX = 20;
    static final int PASSWORD_MIN = 8;
    static final int PASSWORD_MAX = 64;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * 검증 순서는 이메일 → 비밀번호 → 이름으로 고정한다.
     * 여러 필드가 동시에 잘못됐을 때 클라이언트가 받는 코드가 요청마다 달라지면 안 된다.
     */
    public void validate(String email, String rawPassword, String name) {
        String normalizedEmail = Member.normalizeEmail(email);
        if (normalizedEmail == null || !EMAIL.matcher(normalizedEmail).matches()) {
            throw new AppException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
        // 비밀번호는 앞뒤 공백도 사용자가 의도한 문자다 — 다듬지 않고 길이를 잰다.
        if (rawPassword == null || rawPassword.length() < PASSWORD_MIN || rawPassword.length() > PASSWORD_MAX) {
            throw new AppException(AuthErrorCode.INVALID_PASSWORD_LENGTH);
        }
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > NAME_MAX) {
            throw new AppException(AuthErrorCode.INVALID_MEMBER_NAME);
        }
    }
}
