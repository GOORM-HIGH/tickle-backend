package com.profect.tickle.domain.reservation.entity;

import com.profect.tickle.domain.performance.entity.Hall;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "seat_class")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_class_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "seat_class_grade", length = 3, nullable = false)
    private SeatGrade grade;

    @Column(name = "seat_class_amount", nullable = false)
    private Short amount;

    @Column(name = "seat_class_price", nullable = false)
    private Integer price;
}
