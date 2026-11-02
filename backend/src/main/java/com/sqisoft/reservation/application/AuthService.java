package com.sqisoft.reservation.application;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.AuthErrorCode;
import com.sqisoft.reservation.domain.Member;
import com.sqisoft.reservation.domain.MemberPolicy;
import com.sqisoft.reservation.domain.Session;

public class AuthService {

    private final MemberRepository memberRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;
    private final MemberPolicy memberPolicy;
    private final Clock clock;
    private final Duration sessionTtl;

    public AuthService(MemberRepository memberRepository,
            SessionRepository sessionRepository,
            PasswordHasher passwordHasher,
            TokenGenerator tokenGenerator,
            MemberPolicy memberPolicy,
            Clock clock,
            Duration sessionTtl) {
        this.memberRepository = memberRepository;
        this.sessionRepository = sessionRepository;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.memberPolicy = memberPolicy;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    public Member signup(SignupCommand command) {
        memberPolicy.validate(command.email(), command.password(), command.name());

        String email = Member.normalizeEmail(command.email());
        if (memberRepository.existsByEmail(email)) {
            throw new AppException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        Member member = Member.register(email, command.name(),
                passwordHasher.hash(command.password()), LocalDateTime.now(clock));
        return memberRepository.save(member);
    }

    /**
     * 미가입 이메일과 비밀번호 불일치를 같은 코드로 거절한다 —
     * 오류 코드가 갈리면 그것만으로 가입 여부를 캐낼 수 있다.
     */
    public LoginResult login(String email, String rawPassword) {
        Member member = memberRepository.findByEmail(Member.normalizeEmail(email))
                .filter(found -> passwordHasher.matches(rawPassword, found.passwordHash()))
                .orElseThrow(() -> new AppException(AuthErrorCode.INVALID_CREDENTIALS));

        Session session = sessionRepository.save(Session.issue(
                tokenGenerator.generate(), member.id(), LocalDateTime.now(clock), sessionTtl));
        return new LoginResult(session.token(), member);
    }

    public void logout(String token) {
        sessionRepository.deleteByToken(token);
    }

    public Member authenticate(String token) {
        if (token == null) {
            throw new AppException(AuthErrorCode.UNAUTHENTICATED);
        }
        Session session = sessionRepository.findByToken(token)
                .filter(found -> found.isValidAt(LocalDateTime.now(clock)))
                .orElseThrow(() -> new AppException(AuthErrorCode.UNAUTHENTICATED));

        return memberRepository.findById(session.memberId())
                .orElseThrow(() -> new AppException(AuthErrorCode.UNAUTHENTICATED));
    }
}
