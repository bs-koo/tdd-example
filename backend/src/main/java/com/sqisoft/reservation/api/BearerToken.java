package com.sqisoft.reservation.api;

import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.AuthErrorCode;

final class BearerToken {

    private static final String PREFIX = "Bearer ";

    private BearerToken() {
    }

    static String from(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            throw new AppException(AuthErrorCode.UNAUTHENTICATED);
        }
        String token = authorizationHeader.substring(PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AppException(AuthErrorCode.UNAUTHENTICATED);
        }
        return token;
    }
}
