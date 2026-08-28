package com.sqisoft.reservation.support;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 통합 테스트의 시각을 고정한다 (QE-1).
 * 빈 메서드명이 ClockConfig.clock() 과 달라야 BeanDefinitionOverrideException 을 피한다 (C-1).
 */
@TestConfiguration
public class FixedClockConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        return ReservationTestFixtures.FIXED_CLOCK;
    }
}
