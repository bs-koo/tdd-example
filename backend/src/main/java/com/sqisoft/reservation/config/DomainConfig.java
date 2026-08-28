package com.sqisoft.reservation.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sqisoft.reservation.application.ReservationRepository;
import com.sqisoft.reservation.application.ReservationService;
import com.sqisoft.reservation.application.RoomRepository;
import com.sqisoft.reservation.application.RoomService;
import com.sqisoft.reservation.domain.ReservationPolicy;

/**
 * 도메인·애플리케이션 계층은 Spring 애노테이션을 갖지 않는다.
 * 빈 등록은 이 클래스가 전담하여 두 계층이 Spring을 모르는 상태를 유지한다.
 */
@Configuration
public class DomainConfig {

    @Bean
    public ReservationPolicy reservationPolicy(Clock clock) {
        return new ReservationPolicy(clock);
    }

    @Bean
    public ReservationService reservationService(ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            ReservationPolicy reservationPolicy,
            Clock clock) {
        return new ReservationService(reservationRepository, roomRepository, reservationPolicy, clock);
    }

    @Bean
    public RoomService roomService(RoomRepository roomRepository) {
        return new RoomService(roomRepository);
    }
}
