package com.profect.tickle.domain.notification.event.event.reservation;

import com.profect.tickle.domain.reservation.entity.Reservation;
import lombok.Getter;

@Getter
public record ReservationSuccessEvent(Reservation reservation) {
}
