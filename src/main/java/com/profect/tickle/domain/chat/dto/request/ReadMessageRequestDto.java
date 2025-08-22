package com.profect.tickle.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadMessageRequestDto {

    @NotNull(message = "읽은 메시지 ID는 필수입니다")
    private Long lastReadMessageId;  // 완벽함!

    // Lombok @Getter에 의해 자동으로 생성되는 메서드:
    // public Long getLastReadMessageId() { return lastReadMessageId; }
}
