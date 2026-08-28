package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationPolicyTest {

    private ReservationPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ReservationPolicy(ReservationTestFixtures.FIXED_CLOCK);
    }

    @Test
    @DisplayName("AC-4/AC-9: 09:00~09:30(최소 30분, 영업시간 내) 예약은 통과한다")
    void 최소_30분_영업시간_내_예약은_통과한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "09:30");

        assertThatNoException()
                .isThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot));
    }

    @Test
    @DisplayName("AC-5: 30분 단위가 아닌 시작 시각은 INVALID_TIME_UNIT 예외를 던진다")
    void 삼십분_단위가_아니면_INVALID_TIME_UNIT_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:15", "09:45");

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_TIME_UNIT);
    }

    @Test
    @DisplayName("AC-6(경계): 종료 시각이 정확히 18:00이면 통과한다")
    void 종료_시각이_18시_정각이면_통과한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("17:30", "18:00");

        assertThatNoException()
                .isThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot));
    }

    @Test
    @DisplayName("AC-7: 내일(DEFAULT_DATE) 08:30 시작은 영업 시작 전이라 OUTSIDE_BUSINESS_HOURS 예외를 던진다")
    void 영업_시작_전_시각은_OUTSIDE_BUSINESS_HOURS_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("08:30", "09:00");

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.OUTSIDE_BUSINESS_HOURS);
    }

    @Test
    @DisplayName("AC-8: 종료 시각이 18:00을 초과하면 OUTSIDE_BUSINESS_HOURS 예외를 던진다")
    void 종료_시각이_18시_초과면_OUTSIDE_BUSINESS_HOURS_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("18:00", "18:30");

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.OUTSIDE_BUSINESS_HOURS);
    }

    @Test
    @DisplayName("AC-10: 시작과 종료가 같아 0분인 예약은 INVALID_DURATION 예외를 던진다")
    void 예약_시간이_0분이면_INVALID_DURATION_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "09:00");

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_DURATION);
    }

    @Test
    @DisplayName("AC-11(경계): 정확히 4시간인 예약은 통과한다")
    void 정확히_4시간인_예약은_통과한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "13:00");

        assertThatNoException()
                .isThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot));
    }

    @Test
    @DisplayName("AC-12: 4시간을 초과한 예약은 INVALID_DURATION 예외를 던진다")
    void 사시간_초과_예약은_INVALID_DURATION_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "13:30");

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_DURATION);
    }

    @Test
    @DisplayName("AC-13: AC-7과 시·분은 동일(08:30~09:00)하지만 날짜가 오늘(TODAY)이면 PAST_DATETIME이 먼저 발생한다")
    void 오늘_08시30분_시작은_현재보다_과거이므로_PAST_DATETIME_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.slot(ReservationTestFixtures.TODAY, "08:30", "09:00");

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.PAST_DATETIME);
    }

    @Test
    @DisplayName("AC-14(경계): 시작 시각이 현재 시각과 정확히 같으면 과거가 아니므로 통과한다")
    void 시작_시각이_현재_시각과_같으면_통과한다() {
        TimeSlot slot = ReservationTestFixtures.slot(ReservationTestFixtures.TODAY, "09:00", "09:30");

        assertThatNoException()
                .isThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot));
    }

    @Test
    @DisplayName("AC-15(경계): 정확히 14일 후(MAX_DATE) 예약은 통과한다")
    void 정확히_14일_후_예약은_통과한다() {
        TimeSlot slot = ReservationTestFixtures.slot(ReservationTestFixtures.MAX_DATE, "09:00", "10:00");

        assertThatNoException()
                .isThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot));
    }

    @Test
    @DisplayName("AC-16: 14일을 초과한(OVER_MAX_DATE, 15일 후) 예약은 TOO_FAR_IN_FUTURE 예외를 던진다")
    void 십오일_후_예약은_TOO_FAR_IN_FUTURE_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.slot(ReservationTestFixtures.OVER_MAX_DATE, "09:00", "10:00");

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.TOO_FAR_IN_FUTURE);
    }

    @Test
    @DisplayName("AC-18: 예약자명이 빈 문자열이면 INVALID_RESERVER_NAME 예외를 던진다")
    void 예약자명이_빈_문자열이면_INVALID_RESERVER_NAME_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "10:00");

        assertThatThrownBy(() -> policy.validate("", null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_RESERVER_NAME);
    }

    @Test
    @DisplayName("AC-18b(경계): 예약자명이 21자이면 INVALID_RESERVER_NAME 예외를 던진다")
    void 예약자명이_21자이면_INVALID_RESERVER_NAME_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "10:00");
        String tooLongName = "가".repeat(21);

        assertThatThrownBy(() -> policy.validate(tooLongName, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_RESERVER_NAME);
    }

    @Test
    @DisplayName("경계: 예약자명이 정확히 20자이면 통과한다")
    void 예약자명이_정확히_20자이면_통과한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "09:30");
        String exactlyTwentyChars = "가".repeat(20);

        assertThatNoException()
                .isThrownBy(() -> policy.validate(exactlyTwentyChars, null, slot));
    }

    @Test
    @DisplayName("경계: 예약자명이 공백만으로 이루어지면 INVALID_RESERVER_NAME 예외를 던진다")
    void 예약자명이_공백만이면_INVALID_RESERVER_NAME_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "10:00");

        assertThatThrownBy(() -> policy.validate("   ", null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_RESERVER_NAME);
    }

    @Test
    @DisplayName("AC-18c(경계): 목적이 51자이면 INVALID_PURPOSE_LENGTH 예외를 던진다")
    void 목적이_51자이면_INVALID_PURPOSE_LENGTH_예외를_던진다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "10:00");
        String tooLongPurpose = "가".repeat(51);

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, tooLongPurpose, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_PURPOSE_LENGTH);
    }

    @Test
    @DisplayName("경계: 목적이 정확히 50자이면 통과한다")
    void 목적이_정확히_50자이면_통과한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "09:30");
        String exactlyFiftyChars = "가".repeat(50);

        assertThatNoException()
                .isThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, exactlyFiftyChars, slot));
    }

    @Test
    @DisplayName("경계: 목적이 빈 문자열이면 통과한다(선택 필드)")
    void 목적이_빈_문자열이면_통과한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "09:30");

        assertThatNoException()
                .isThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, "", slot));
    }

    @Test
    @DisplayName("경계: 시작 시각의 초가 0이 아니면 INVALID_TIME_UNIT 예외를 던진다")
    void 시작_시각의_초가_0이_아니면_INVALID_TIME_UNIT_예외를_던진다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00").withSecond(30);
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        TimeSlot slot = new TimeSlot(start, end);

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_TIME_UNIT);
    }

    @Test
    @DisplayName("경계: 시작 시각의 나노초가 0이 아니면 INVALID_TIME_UNIT 예외를 던진다")
    void 시작_시각의_나노초가_0이_아니면_INVALID_TIME_UNIT_예외를_던진다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00").withNano(1);
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        TimeSlot slot = new TimeSlot(start, end);

        assertThatThrownBy(() -> policy.validate(ReservationTestFixtures.RESERVER, null, slot))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_TIME_UNIT);
    }
}
