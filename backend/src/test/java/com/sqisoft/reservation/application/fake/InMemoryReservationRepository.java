package com.sqisoft.reservation.application.fake;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.sqisoft.reservation.application.ReservationRepository;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationStatus;

/**
 * 테스트 전용 인메모리 {@link ReservationRepository} Fake 구현.
 * 프로덕션 코드가 아니며, JPA 어댑터의 계약(§9.5)을 검증하기 위한 목적으로만 사용한다.
 */
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<Long, Reservation> store = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Reservation save(Reservation reservation) {
        if (reservation.id() == null) {
            long newId = sequence.incrementAndGet();
            Reservation restored = Reservation.restore(
                    newId,
                    reservation.roomId(),
                    reservation.reserverName(),
                    reservation.purpose(),
                    reservation.timeSlot(),
                    reservation.status(),
                    reservation.createdAt());
            store.put(newId, restored);
            return restored;
        }

        store.put(reservation.id(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryReservationRepository::copyOf);
    }

    /**
     * 저장소 내부 객체가 아닌 준영속 복사본을 반환하기 위한 헬퍼.
     * 실제 JPA에서 새로 조회하면 새 인스턴스를 받고, 그걸 변이해도 merge/flush 없이는
     * 저장소에 반영되지 않는다(§4.3 Fake↔JPA 동치성). 같은 참조를 그대로 돌려주면
     * 호출자의 변이가 save() 없이도 저장소를 오염시켜 save() 누락을 검증하지 못하게 된다.
     */
    private static Reservation copyOf(Reservation reservation) {
        return Reservation.restore(
                reservation.id(),
                reservation.roomId(),
                reservation.reserverName(),
                reservation.purpose(),
                reservation.timeSlot(),
                reservation.status(),
                reservation.createdAt());
    }

    @Override
    public List<Reservation> findActiveByRoomIdAndDate(Long roomId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();

        return store.values().stream()
                .filter(reservation -> reservation.roomId().equals(roomId))
                .filter(reservation -> reservation.status() == ReservationStatus.ACTIVE)
                .filter(reservation -> {
                    LocalDateTime start = reservation.timeSlot().start();
                    return !start.isBefore(startOfDay) && start.isBefore(startOfNextDay);
                })
                .sorted(Comparator.comparing(reservation -> reservation.timeSlot().start()))
                .map(InMemoryReservationRepository::copyOf)
                .collect(Collectors.toList());
    }

    /**
     * 초기 데이터를 적재한다. id가 없는 인스턴스는 허용하지 않으며,
     * 적재된 id만큼 시퀀스를 전진시켜 이후 save()가 기존 id를 덮어쓰지 않도록 한다.
     */
    public void seed(Reservation... reservations) {
        for (Reservation reservation : reservations) {
            if (reservation.id() == null) {
                throw new IllegalArgumentException("seed에는 id가 있는 예약만 사용할 수 있습니다.");
            }
            store.put(reservation.id(), reservation);
            sequence.updateAndGet(current -> Math.max(current, reservation.id()));
        }
    }
}
