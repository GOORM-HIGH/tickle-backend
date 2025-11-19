package com.profect.tickle.domain.event.service.stream.dto;

import java.io.Serializable;

public record EventMessage(Long eventId, Long memberId) implements Serializable {}