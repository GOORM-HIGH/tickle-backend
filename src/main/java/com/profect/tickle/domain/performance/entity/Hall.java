package com.profect.tickle.domain.performance.entity;

import jakarta.persistence.GenerationType;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "hall")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hall_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "hall_type", length = 10, nullable = false)
    private HallType type;

    @Column(name = "hall_address", length = 50, nullable = false)
    private String address;
}
