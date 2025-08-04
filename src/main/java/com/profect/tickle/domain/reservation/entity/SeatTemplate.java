package com.profect.tickle.domain.reservation.entity;

import com.profect.tickle.domain.performance.entity.HallType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "seat_grade", length = 3, nullable = false)
    private SeatGrade seatGrade;

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber;

    @Column(name = "price", nullable = false)
    private Integer price;
}
