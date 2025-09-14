package com.profect.tickle.domain.event.stream.dto;

import java.io.Serializable;

public record EventMessage(Long eventId, Long memberId) implements Serializable {}