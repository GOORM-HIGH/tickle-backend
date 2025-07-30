package com.profect.tickle.domain.chat.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;  // ✅ 올바른 import

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)  // ✅ 이제 작동함
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Test
    void 공연별_채팅방_조회_테스트() {
        // given
        Long performanceId = 1L;

        // when
        boolean exists = chatRoomRepository.existsByPerformanceId(performanceId);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void 활성화된_채팅방_조회_테스트() {
        // when
        var activeChatRooms = chatRoomRepository.findByStatusTrue();

        // then
        assertThat(activeChatRooms).isEmpty();
    }
}
