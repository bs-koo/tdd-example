package com.sqisoft.reservation.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sqisoft.reservation.application.AuthService;
import com.sqisoft.reservation.application.GroupAccess;
import com.sqisoft.reservation.application.GroupMembershipRepository;
import com.sqisoft.reservation.application.GroupRepository;
import com.sqisoft.reservation.application.GroupService;
import com.sqisoft.reservation.application.MemberRepository;
import com.sqisoft.reservation.application.PasswordHasher;
import com.sqisoft.reservation.application.ReservationRepository;
import com.sqisoft.reservation.application.ReservationService;
import com.sqisoft.reservation.application.RoomRepository;
import com.sqisoft.reservation.application.RoomService;
import com.sqisoft.reservation.application.SessionRepository;
import com.sqisoft.reservation.application.TokenGenerator;
import com.sqisoft.reservation.domain.MemberPolicy;
import com.sqisoft.reservation.domain.ReservationPolicy;
import com.sqisoft.reservation.infra.Pbkdf2PasswordHasher;
import com.sqisoft.reservation.infra.SecureRandomTokenGenerator;

/**
 * 도메인·애플리케이션 계층은 Spring 애노테이션을 갖지 않는다.
 * 빈 등록은 이 클래스가 전담하여 두 계층이 Spring을 모르는 상태를 유지한다.
 */
@Configuration
public class DomainConfig {

    /** 세션 유효 기간. 근무일 하루를 넘기지 않도록 12시간으로 둔다. */
    private static final Duration SESSION_TTL = Duration.ofHours(12);

    @Bean
    public MemberPolicy memberPolicy() {
        return new MemberPolicy();
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new Pbkdf2PasswordHasher();
    }

    @Bean
    public TokenGenerator tokenGenerator() {
        return new SecureRandomTokenGenerator();
    }

    @Bean
    public AuthService authService(MemberRepository memberRepository,
            SessionRepository sessionRepository,
            PasswordHasher passwordHasher,
            TokenGenerator tokenGenerator,
            MemberPolicy memberPolicy,
            Clock clock) {
        return new AuthService(memberRepository, sessionRepository, passwordHasher, tokenGenerator,
                memberPolicy, clock, SESSION_TTL);
    }

    @Bean
    public GroupAccess groupAccess(GroupRepository groupRepository,
            GroupMembershipRepository groupMembershipRepository) {
        return new GroupAccess(groupRepository, groupMembershipRepository);
    }

    @Bean
    public GroupService groupService(GroupRepository groupRepository,
            GroupMembershipRepository groupMembershipRepository,
            MemberRepository memberRepository,
            GroupAccess groupAccess,
            Clock clock) {
        return new GroupService(groupRepository, groupMembershipRepository, memberRepository,
                groupAccess, clock);
    }

    @Bean
    public RoomService roomService(RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            GroupAccess groupAccess,
            Clock clock) {
        return new RoomService(roomRepository, reservationRepository, groupAccess, clock);
    }

    @Bean
    public ReservationPolicy reservationPolicy(Clock clock) {
        return new ReservationPolicy(clock);
    }

    @Bean
    public ReservationService reservationService(ReservationRepository reservationRepository,
            RoomService roomService,
            ReservationPolicy reservationPolicy,
            Clock clock) {
        return new ReservationService(reservationRepository, roomService, reservationPolicy, clock);
    }
}
