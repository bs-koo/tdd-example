package com.sqisoft.reservation.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sqisoft.reservation.application.AuthService;
import com.sqisoft.reservation.application.CreateRoomCommand;
import com.sqisoft.reservation.application.GroupService;
import com.sqisoft.reservation.application.MemberRepository;
import com.sqisoft.reservation.application.RoomService;
import com.sqisoft.reservation.application.SignupCommand;
import com.sqisoft.reservation.domain.Member;

/**
 * 인메모리 DB는 재시작마다 비므로 로그인할 계정조차 없다. 개발·시연용 최소 데이터를 심는다.
 * data.sql 대신 서비스를 거치는 이유는 비밀번호 해시를 SQL에 하드코딩하지 않기 위해서다.
 * 테스트 프로파일에서는 각 테스트가 자기 데이터를 만들므로 실행하지 않는다.
 */
@Component
@Profile("!test")
public class DemoDataInitializer implements ApplicationRunner {

    private static final String OWNER_EMAIL = "owner@sqisoft.com";
    private static final String MEMBER_EMAIL = "member@sqisoft.com";
    private static final String DEMO_PASSWORD = "password123";

    private final AuthService authService;
    private final GroupService groupService;
    private final RoomService roomService;
    private final MemberRepository memberRepository;

    public DemoDataInitializer(AuthService authService, GroupService groupService,
            RoomService roomService, MemberRepository memberRepository) {
        this.authService = authService;
        this.groupService = groupService;
        this.roomService = roomService;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByEmail(OWNER_EMAIL)) {
            return;
        }

        Member owner = authService.signup(new SignupCommand(OWNER_EMAIL, DEMO_PASSWORD, "김본승"));
        Member member = authService.signup(new SignupCommand(MEMBER_EMAIL, DEMO_PASSWORD, "박그룹"));

        Long groupId = groupService.createGroup(owner.id(), "SQI 개발팀").group().id();
        Long membershipId = groupService.invite(groupId, owner.id(), MEMBER_EMAIL).membership().id();
        groupService.accept(membershipId, member.id());

        roomService.createRoom(groupId, owner.id(), new CreateRoomCommand("대회의실", 20, "3층"));
        roomService.createRoom(groupId, owner.id(), new CreateRoomCommand("중회의실", 10, "3층"));
        roomService.createRoom(groupId, owner.id(), new CreateRoomCommand("소회의실1", 6, "5층"));
        roomService.createRoom(groupId, owner.id(), new CreateRoomCommand("소회의실2", 6, "5층"));
    }
}
