package com.sqisoft.reservation.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.application.fake.InMemoryMemberRepository;
import com.sqisoft.reservation.application.fake.InMemorySessionRepository;
import com.sqisoft.reservation.application.fake.SequentialTokenGenerator;
import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.AuthErrorCode;
import com.sqisoft.reservation.domain.Member;
import com.sqisoft.reservation.domain.MemberPolicy;
import com.sqisoft.reservation.infra.Pbkdf2PasswordHasher;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private static final String EMAIL = "bonseung@sqisoft.com";
    private static final String PASSWORD = "password123";
    private static final String NAME = "김본승";
    private static final Duration TTL = Duration.ofHours(12);

    private InMemoryMemberRepository memberRepository;
    private InMemorySessionRepository sessionRepository;
    private SequentialTokenGenerator tokenGenerator;
    private AuthService service;

    @BeforeEach
    void setUp() {
        memberRepository = new InMemoryMemberRepository();
        sessionRepository = new InMemorySessionRepository();
        tokenGenerator = new SequentialTokenGenerator();
        service = serviceAt(ReservationTestFixtures.FIXED_CLOCK);
    }

    /** 같은 저장소를 공유하되 시각만 다른 서비스 — 토큰 만료를 관찰하기 위한 것이다. */
    private AuthService serviceAt(Clock clock) {
        return new AuthService(memberRepository, sessionRepository, new Pbkdf2PasswordHasher(),
                tokenGenerator, new MemberPolicy(), clock, TTL);
    }

    private AuthService serviceAfter(Duration elapsed) {
        return serviceAt(Clock.fixed(ReservationTestFixtures.FIXED_INSTANT.plus(elapsed),
                ReservationTestFixtures.ZONE));
    }

    // ── 가입 ──

    @Test
    @DisplayName("가입하면 정규화된 이메일과 이름을 가진 회원이 id와 함께 저장된다")
    void 가입하면_회원이_id와_함께_저장된다() {
        Member member = service.signup(new SignupCommand("  BonSeung@SQIsoft.com ", PASSWORD, " 김본승 "));

        assertThat(member.id()).isNotNull();
        assertThat(member.email()).isEqualTo(EMAIL);
        assertThat(member.name()).isEqualTo(NAME);
        assertThat(memberRepository.findByEmail(EMAIL)).isPresent();
    }

    @Test
    @DisplayName("가입한 비밀번호는 평문으로 저장되지 않는다")
    void 비밀번호는_평문으로_저장되지_않는다() {
        Member member = service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        assertThat(member.passwordHash()).isNotEqualTo(PASSWORD);
        assertThat(member.passwordHash()).doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("이미 가입된 이메일로 다시 가입하면 EMAIL_ALREADY_REGISTERED를 던진다")
    void 중복_이메일로_가입하면_거절한다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        assertThatThrownBy(() -> service.signup(new SignupCommand(EMAIL, PASSWORD, "다른이름")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("대소문자만 다른 이메일로 가입하면 EMAIL_ALREADY_REGISTERED를 던진다")
    void 대소문자만_다른_이메일도_중복이다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        assertThatThrownBy(() -> service.signup(new SignupCommand("BONSEUNG@SQISOFT.COM", PASSWORD, NAME)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("형식이 잘못된 이메일로 가입하면 중복 검사 전에 INVALID_EMAIL_FORMAT을 던진다")
    void 형식_검증이_중복_검사보다_먼저다() {
        assertThatThrownBy(() -> service.signup(new SignupCommand("bad-email", PASSWORD, NAME)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    // ── 로그인 ──

    @Test
    @DisplayName("올바른 자격으로 로그인하면 토큰과 회원 정보를 돌려준다")
    void 로그인에_성공하면_토큰을_돌려준다() {
        Member registered = service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        LoginResult result = service.login(EMAIL, PASSWORD);

        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.member().id()).isEqualTo(registered.id());
        assertThat(result.member().name()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("로그인 시 이메일 대소문자는 구분하지 않는다")
    void 로그인_이메일은_대소문자를_구분하지_않는다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        LoginResult result = service.login("BonSeung@SQIsoft.COM", PASSWORD);

        assertThat(result.member().email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS를 던진다")
    void 비밀번호가_틀리면_거절한다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        assertThatThrownBy(() -> service.login(EMAIL, "wrong-password"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("가입하지 않은 이메일로 로그인해도 비밀번호 오류와 같은 INVALID_CREDENTIALS를 던진다 — 가입 여부가 새지 않는다")
    void 미가입_이메일도_같은_오류로_거절한다() {
        assertThatThrownBy(() -> service.login("nobody@sqisoft.com", PASSWORD))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("로그인에 실패하면 세션이 발급되지 않는다")
    void 로그인_실패는_세션을_남기지_않는다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        assertThatThrownBy(() -> service.login(EMAIL, "wrong-password"))
                .isInstanceOf(AppException.class);

        assertThat(sessionRepository.findByToken("token-1")).isEmpty();
    }

    @Test
    @DisplayName("두 번 로그인하면 서로 다른 토큰이 발급되고 둘 다 유효하다")
    void 두_번_로그인하면_두_토큰_모두_유효하다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));

        String first = service.login(EMAIL, PASSWORD).token();
        String second = service.login(EMAIL, PASSWORD).token();

        assertThat(first).isNotEqualTo(second);
        assertThat(service.authenticate(first).email()).isEqualTo(EMAIL);
        assertThat(service.authenticate(second).email()).isEqualTo(EMAIL);
    }

    // ── 인증 ──

    @Test
    @DisplayName("발급된 토큰으로 인증하면 로그인한 회원을 돌려준다")
    void 발급된_토큰으로_인증한다() {
        Member registered = service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));
        String token = service.login(EMAIL, PASSWORD).token();

        Member authenticated = service.authenticate(token);

        assertThat(authenticated.id()).isEqualTo(registered.id());
    }

    @Test
    @DisplayName("발급된 적 없는 토큰으로 인증하면 UNAUTHENTICATED를 던진다")
    void 없는_토큰은_거절한다() {
        assertThatThrownBy(() -> service.authenticate("존재하지-않는-토큰"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("null 토큰으로 인증하면 UNAUTHENTICATED를 던진다")
    void null_토큰은_거절한다() {
        assertThatThrownBy(() -> service.authenticate(null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("TTL이 지나기 전에는 토큰이 유효하다")
    void TTL_이내의_토큰은_유효하다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));
        String token = service.login(EMAIL, PASSWORD).token();

        Member authenticated = serviceAfter(Duration.ofHours(11)).authenticate(token);

        assertThat(authenticated.email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("TTL이 지난 토큰으로 인증하면 UNAUTHENTICATED를 던진다")
    void 만료된_토큰은_거절한다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));
        String token = service.login(EMAIL, PASSWORD).token();

        AuthService later = serviceAfter(Duration.ofHours(13));

        assertThatThrownBy(() -> later.authenticate(token))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("발급된 세션의 만료 시각은 로그인 시각에 TTL을 더한 값이다")
    void 세션_만료_시각은_로그인_시각_더하기_TTL이다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));
        String token = service.login(EMAIL, PASSWORD).token();

        assertThat(sessionRepository.findByToken(token)).get()
                .extracting(session -> session.expiresAt())
                .isEqualTo(LocalDateTime.ofInstant(
                        Instant.parse("2026-08-25T00:00:00Z").plus(TTL), ReservationTestFixtures.ZONE));
    }

    // ── 로그아웃 ──

    @Test
    @DisplayName("로그아웃하면 그 토큰으로는 더 이상 인증되지 않는다")
    void 로그아웃한_토큰은_거절한다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));
        String token = service.login(EMAIL, PASSWORD).token();

        service.logout(token);

        assertThatThrownBy(() -> service.authenticate(token))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("로그아웃은 다른 기기에서 발급된 토큰까지 무효화하지는 않는다")
    void 로그아웃은_해당_토큰만_무효화한다() {
        service.signup(new SignupCommand(EMAIL, PASSWORD, NAME));
        String first = service.login(EMAIL, PASSWORD).token();
        String second = service.login(EMAIL, PASSWORD).token();

        service.logout(first);

        assertThat(service.authenticate(second).email()).isEqualTo(EMAIL);
    }
}
