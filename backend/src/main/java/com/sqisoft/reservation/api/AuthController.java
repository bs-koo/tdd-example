package com.sqisoft.reservation.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqisoft.reservation.api.dto.LoginRequest;
import com.sqisoft.reservation.api.dto.LoginResponse;
import com.sqisoft.reservation.api.dto.MemberResponse;
import com.sqisoft.reservation.api.dto.SignupRequest;
import com.sqisoft.reservation.application.AuthService;
import com.sqisoft.reservation.domain.Member;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@RequestBody SignupRequest request) {
        return MemberResponse.from(authService.signup(request.toCommand()));
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return LoginResponse.from(authService.login(request.email(), request.password()));
    }

    /**
     * {@code @LoginMember} 를 함께 받는 이유는 무효한 토큰으로 온 로그아웃을 204로 삼키지 않기 위해서다.
     * 실제로 지울 토큰 문자열은 헤더에서 다시 꺼낸다.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@LoginMember Member member,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(BearerToken.from(authorization));
    }

    @GetMapping("/me")
    public MemberResponse me(@LoginMember Member member) {
        return MemberResponse.from(member);
    }
}
