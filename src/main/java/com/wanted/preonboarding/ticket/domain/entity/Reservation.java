package com.wanted.preonboarding.ticket.domain.entity;

import com.wanted.preonboarding.ticket.domain.dto.RequestReservation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Builder
@Comment("예약 정보")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservation", uniqueConstraints = {
        @UniqueConstraint(name = "reservation_uk", columnNames = {"round", "line", "seat"})
})
public class Reservation extends BaseEntity {

    @Id
    @Comment("예약 정보 ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Comment("공연 좌석 정보")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_seat_info_id", nullable = false)
    private PerformanceSeatInfo performanceSeatInfo;

    @Comment("예약자 이름")
    @Column(name = "name", nullable = false)
    private String name;

    @Comment("예약자 휴대전화 번호")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Comment("회차 정보")
    @Column(name = "round", nullable = false)
    private Integer round;

    @Comment("입장 게이트")
    @Column(name = "gate", nullable = false)
    private Integer gate;

    @Comment("좌석 열")
    @Column(name = "line", nullable = false)
    private Character line;

    @Comment("좌석 행")
    @Column(name = "seat", nullable = false)
    private Integer seat;

    public static Reservation of(RequestReservation requestReservation, PerformanceSeatInfo seatInfo) {
        return Reservation.builder()
                .performanceSeatInfo(seatInfo)
                .name(requestReservation.name())
                .phoneNumber(requestReservation.phoneNumber())
                .round(requestReservation.round())
                .gate(1)
                .line(requestReservation.line())
                .seat(requestReservation.seat())
                .build();
    }

}
