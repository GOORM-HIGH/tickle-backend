package com.profect.tickle.domain.event.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "ticket_applied")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TicketApplied {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private Long memberId;
    private boolean winner;
}