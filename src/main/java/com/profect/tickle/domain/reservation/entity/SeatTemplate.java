package com.profect.tickle.domain.reservation.entity;

import jakarta.persistence.*;
import com.profect.tickle.domain.performance.entity.HallType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "seat_template")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatTemplate {

    @Id
    @Column(name = "seat_template_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "hall_type", length = 10, nullable = false)
    private HallType hallType;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_grade", length = 3, nullable = false)
    private SeatGrade seatGrade;

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber;

    @Column(name = "price", nullable = false)
    private Integer price;
}
