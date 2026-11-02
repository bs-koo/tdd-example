package com.sqisoft.reservation.domain;

public enum AuthErrorCode implements ErrorCode {

    UNAUTHENTICATED("로그인이 필요합니다."),
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다."),
    EMAIL_ALREADY_REGISTERED("이미 가입된 이메일입니다."),
    INVALID_EMAIL_FORMAT("이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_LENGTH("비밀번호는 8자 이상 64자 이하로 입력해주세요."),
    INVALID_MEMBER_NAME("이름은 1~20자로 입력해주세요."),
    MEMBER_NOT_FOUND("해당 이메일의 사용자를 찾을 수 없습니다.");

    private final String message;

    AuthErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
