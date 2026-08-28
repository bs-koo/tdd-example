package com.sqisoft.reservation.api;

import com.sqisoft.reservation.api.support.ApiTestSupport;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomApiTest extends ApiTestSupport {

    @Test
    @DisplayName("AC-25: 회의실 전체 목록을 id 오름차순으로 반환하며 시드 데이터와 정확히 일치한다")
    void 회의실_전체_목록을_id_오름차순으로_반환한다() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("대회의실"))
                .andExpect(jsonPath("$[0].capacity").value(20))
                .andExpect(jsonPath("$[0].location").value("3층"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("중회의실"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].name").value("소회의실1"))
                .andExpect(jsonPath("$[3].id").value(4))
                .andExpect(jsonPath("$[3].name").value("소회의실2"))
                .andExpect(jsonPath("$[3].capacity").value(6))
                .andExpect(jsonPath("$[3].location").value("5층"));
    }

    @Test
    @DisplayName("AC-26: 지정한 날짜의 예약만 반환하고 다른 날짜의 예약은 제외한다")
    void 지정한_날짜의_예약만_반환한다() throws Exception {
        Reservation onTargetDate = Reservation.create(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "당일 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.TODAY, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "08:00"));
        Reservation onOtherDate = Reservation.create(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "다음날 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(onTargetDate);
        em.persist(onOtherDate);
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", ReservationTestFixtures.ROOM_ID)
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].purpose").value("당일 회의"));
    }

    @Test
    @DisplayName("AC-27: 취소된 예약은 조회 결과에서 제외되고 ACTIVE 예약만 포함된다")
    void 취소된_예약은_조회_결과에서_제외된다() throws Exception {
        Reservation active = Reservation.create(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "정상 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.TODAY, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "08:00"));
        Reservation cancelled = Reservation.create(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "취소될 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.TODAY, "11:00", "12:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "08:00"));
        em.persist(active);
        em.persist(cancelled);
        cancelled.cancel(ReservationTestFixtures.RESERVER);
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", ReservationTestFixtures.ROOM_ID)
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].purpose").value("정상 회의"));
    }

    @Test
    @DisplayName("AC-28: 예약이 없는 날짜는 404가 아니라 200과 빈 배열을 반환한다")
    void 예약이_없는_날짜는_빈_배열을_반환한다() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", ReservationTestFixtures.ROOM_ID)
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("AC-26b: 존재하지 않는 회의실을 조회하면 404와 ROOM_NOT_FOUND 코드를 반환한다")
    void 존재하지_않는_회의실을_조회하면_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", 999L)
                        .param("date", "2026-08-26"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-29: 날짜 형식이 잘못되면 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void 날짜_형식이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", ReservationTestFixtures.ROOM_ID)
                        .param("date", "2026-13-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("AC-29 우선순위: 날짜 형식 오류와 회의실 부재가 동시에 발생하면 INVALID_DATE_FORMAT이 우선한다")
    void 날짜_형식_오류와_회의실_부재가_동시_발생하면_날짜_형식_오류가_우선한다() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", 999L)
                        .param("date", "2026-13-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("예약 조회 응답은 startAt·endAt을 초까지 포함한 문자열로, status를 ACTIVE로 반환한다")
    void 예약_조회_응답의_시각_필드는_초까지_포함되고_상태는_ACTIVE로_반환된다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.TODAY, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "08:00"));
        em.persist(reservation);
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", ReservationTestFixtures.ROOM_ID)
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startAt").value("2026-08-25T09:00:00"))
                .andExpect(jsonPath("$[0].endAt").value("2026-08-25T10:00:00"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
