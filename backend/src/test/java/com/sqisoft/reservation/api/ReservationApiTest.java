package com.sqisoft.reservation.api;

import com.sqisoft.reservation.api.support.ApiTestSupport;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

class ReservationApiTest extends ApiTestSupport {

    @Test
    @DisplayName("AC-17: 유효한 예약 요청은 201과 함께 예약 정보 7필드를 반환한다")
    void 유효한_예약_요청은_201과_함께_예약_정보_7필드를_반환한다() throws Exception {
        String requestBody = """
                {"roomId":1,"reserverName":"김본승","purpose":"주간 회의",
                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.reserverName").value("김본승"))
                .andExpect(jsonPath("$.purpose").value("주간 회의"))
                .andExpect(jsonPath("$.startAt").value("2026-08-26T09:00:00"))
                .andExpect(jsonPath("$.endAt").value("2026-08-26T10:00:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @ParameterizedTest(name = "{0}: {1}~{2} 예약 요청은 201을 반환한다")
    @DisplayName("정책상 허용되는 예약 요청 6건(AC-4,6,9,11,14,15)은 모두 201을 반환한다")
    @CsvSource({
            "AC-4, 2026-08-26T09:00:00, 2026-08-26T09:30:00",
            "AC-6, 2026-08-26T17:30:00, 2026-08-26T18:00:00",
            "AC-9, 2026-08-26T09:00:00, 2026-08-26T09:30:00",
            "AC-11, 2026-08-26T09:00:00, 2026-08-26T13:00:00",
            "AC-14, 2026-08-25T09:00:00, 2026-08-25T09:30:00",
            "AC-15, 2026-09-08T09:00:00, 2026-09-08T10:00:00",
    })
    void 정책상_허용되는_예약_요청은_201을_반환한다(String acId, String startAt, String endAt) throws Exception {
        String requestBody = """
                {"roomId":1,"reserverName":"김본승","purpose":"주간 회의","startAt":"%s","endAt":"%s"}
                """.formatted(startAt, endAt);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-20: 본인이 취소를 요청하면 204를 반환한다")
    void 본인이_취소를_요청하면_204를_반환한다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(reservation);
        em.flush();
        Long id = reservation.id();

        String requestBody = """
                {"reserverName":"김본승"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("AC-20: 본인의 취소 요청이 성공하면 예약 상태가 CANCELLED로 영속화된다")
    void 본인의_취소_요청이_성공하면_예약_상태가_CANCELLED로_영속화된다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(reservation);
        em.flush();
        Long id = reservation.id();

        String requestBody = """
                {"reserverName":"김본승"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        em.flush();
        em.clear();

        Reservation reloaded = em.find(Reservation.class, id);
        assertThat(reloaded.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("AC-21: 타인이 취소를 요청하면 403과 NOT_RESERVER 코드를 반환한다")
    void 타인이_취소를_요청하면_403과_NOT_RESERVER_코드를_반환한다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(reservation);
        em.flush();
        Long id = reservation.id();

        String requestBody = """
                {"reserverName":"박철수"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_RESERVER"));
    }

    @Test
    @DisplayName("AC-21: 타인의 취소 요청이 거절되면 예약 상태는 ACTIVE로 유지된다")
    void 타인의_취소_요청이_거절되면_예약_상태는_ACTIVE로_유지된다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(reservation);
        em.flush();
        Long id = reservation.id();

        String requestBody = """
                {"reserverName":"박철수"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        em.flush();
        em.clear();

        Reservation reloaded = em.find(Reservation.class, id);
        assertThat(reloaded.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-22: 이미 취소된 예약을 본인 이름으로 다시 취소하면 409와 ALREADY_CANCELLED 코드를 반환한다")
    void 이미_취소된_예약을_본인_이름으로_다시_취소하면_409와_ALREADY_CANCELLED_코드를_반환한다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(reservation);
        reservation.cancel(ReservationTestFixtures.RESERVER);
        em.flush();
        Long id = reservation.id();

        String requestBody = """
                {"reserverName":"김본승"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CANCELLED"));
    }

    @Test
    @DisplayName("AC-23: 존재하지 않는 예약을 취소하면 404와 RESERVATION_NOT_FOUND 코드를 반환한다")
    void 존재하지_않는_예약을_취소하면_404와_RESERVATION_NOT_FOUND_코드를_반환한다() throws Exception {
        String requestBody = """
                {"reserverName":"김본승"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }
}
