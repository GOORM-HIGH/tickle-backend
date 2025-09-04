package com.profect.tickle.global.paging;

import java.time.Instant;

public record Cursor(Instant lastDate, Long lastId) {}
