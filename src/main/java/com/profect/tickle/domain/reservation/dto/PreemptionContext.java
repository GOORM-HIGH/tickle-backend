package com.profect.tickle.domain.reservation.dto;

import com.profect.tickle.domain.member.entity.Member;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PreemptionContext {
    private String preemptionToken;
    private Instant preemptedAt;
    private Instant preemptedUntil;
    private Member member;
}
