package com.profect.tickle.domain.performance.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "genre")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @Column(name = "genre_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "genre_title", length = 50, nullable = false,unique = true)
    private String title;
}
