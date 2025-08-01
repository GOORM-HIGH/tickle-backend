package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatRoomJoinRequestDto;
import com.profect.tickle.domain.chat.dto.request.ReadMessageRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatParticipantsResponseDto;
import com.profect.tickle.domain.chat.dto.response.UnreadCountResponseDto;
import com.profect.tickle.domain.chat.entity.ChatParticipants;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.chat.mapper.ChatParticipantsMapper;
import com.profect.tickle.domain.chat.repository.ChatParticipantsRepository;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.exception.ChatExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatParticipantsService {

    private final ChatParticipantsRepository chatParticipantsRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatParticipantsMapper chatParticipantsMapper; // MyBatis

    /**
     * 채팅방 참여 (JPA 사용)
     */
    @Transactional
    public ChatParticipantsResponseDto joinChatRoom(Long chatRoomId, Long memberId, ChatRoomJoinRequestDto requestDto) {
        log.info("채팅방 참여 요청: chatRoomId={}, memberId={}", chatRoomId, memberId);

        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatExceptions.chatRoomNotFound(chatRoomId));

        // 2. 회원 존재 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ChatExceptions.memberNotFoundInChat(memberId));

        // 3. 기존 참여 여부 확인
        Optional<ChatParticipants> existingParticipant = chatParticipantsRepository
                .findByChatRoomAndMember(chatRoom, member);

        if (existingParticipant.isPresent()) {
            ChatParticipants participant = existingParticipant.get();

            if (participant.getStatus()) {
                // ✅ 올바른 메시지 + 기존 방식으로 DTO 변환
                log.info("사용자가 이미 채팅방에 참여 중: participantId={}", participant.getId());

                // ✅ 기존 방식: 엔티티에서 직접 DTO 생성
                return ChatParticipantsResponseDto.builder()
                        .id(participant.getId())
                        .chatRoomId(participant.getChatRoom().getId())
                        .memberId(participant.getMember().getId())
                        .joinedAt(participant.getJoinedAt())
                        .status(participant.getStatus())
                        .lastReadAt(participant.getLastReadAt())
                        .lastReadMessageId(participant.getLastReadMessageId())
                        .build();

            } else {
                // 비활성 상태였다면 재활성화
                participant.reactivate(); // 이 메서드가 없다면 participant.setStatus(true) 사용
                ChatParticipants saved = chatParticipantsRepository.save(participant);
                log.info("채팅방 재참여 완료: participantId={}", saved.getId());

                // ✅ 재활성화된 참여자 DTO 반환
                return ChatParticipantsResponseDto.builder()
                        .id(saved.getId())
                        .chatRoomId(saved.getChatRoom().getId())
                        .memberId(saved.getMember().getId())
                        .joinedAt(saved.getJoinedAt())
                        .status(saved.getStatus())
                        .lastReadAt(saved.getLastReadAt())
                        .lastReadMessageId(saved.getLastReadMessageId())
                        .build();
            }
        }

        // 4. 채팅방 정원 확인
        int currentParticipants = chatParticipantsRepository.countByChatRoomAndStatusTrue(chatRoom);
        if (currentParticipants >= chatRoom.getMaxParticipants()) {
            throw new IllegalStateException("채팅방 정원이 가득 찼습니다.");
        }

        // 5. 새로운 참여자 생성 (기존 코드 유지)
        ChatParticipants newParticipant = ChatParticipants.builder()
                .chatRoom(chatRoom)
                .member(member)
                .status(true)
                .joinedAt(Instant.now())
                .build();

        ChatParticipants saved = chatParticipantsRepository.save(newParticipant);
        log.info("채팅방 참여 완료: participantId={}", saved.getId());

        // ✅ 기존 방식: 새 참여자 DTO 반환
        return ChatParticipantsResponseDto.builder()
                .id(saved.getId())
                .chatRoomId(saved.getChatRoom().getId())
                .memberId(saved.getMember().getId())
                .joinedAt(saved.getJoinedAt())
                .status(saved.getStatus())
                .lastReadAt(saved.getLastReadAt())
                .lastReadMessageId(saved.getLastReadMessageId())
                .build();
    }


    /**
     * 채팅방 나가기 (JPA 사용)
     */
    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long memberId) {
        log.info("채팅방 나가기 요청: chatRoomId={}, memberId={}", chatRoomId, memberId);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatExceptions.chatRoomNotFound(chatRoomId)); // ✅ 수정

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ChatExceptions.memberNotFoundInChat(memberId)); // ✅ 수정

        ChatParticipants participant = chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> ChatExceptions.chatParticipantNotFound(chatRoomId, memberId)); // ✅ 수정

        // 상태를 비활성화 (논리 삭제)
        participant.leave(); // Entity에 추가할 메서드

        log.info("채팅방 나가기 완료: participantId={}", participant.getId());
    }

    /**
     * 읽음 처리 (MyBatis 사용)
     */
    @Transactional
    public void markAsRead(Long chatRoomId, Long memberId, ReadMessageRequestDto requestDto) {
        log.info("읽음 처리 요청: chatRoomId={}, memberId={}, messageId={}",
                chatRoomId, memberId, requestDto.getLastReadMessageId());

        // MyBatis로 효율적인 업데이트
        int updated = chatParticipantsMapper.updateLastReadMessage(
                chatRoomId,
                memberId,
                requestDto.getLastReadMessageId(),
                Instant.now()
        );

        if (updated == 0) {
            throw ChatExceptions.chatParticipantNotFound(chatRoomId, memberId); // ✅ 수정
        }

        log.info("읽음 처리 완료");
    }

    /**
     * 읽지않은 메시지 개수 조회 (MyBatis 사용)
     */
    public UnreadCountResponseDto getUnreadCount(Long chatRoomId, Long memberId) {
        log.info("읽지않은 메시지 개수 조회: chatRoomId={}, memberId={}", chatRoomId, memberId);

        UnreadCountResponseDto result = chatParticipantsMapper.getReadStatus(chatRoomId, memberId);

        if (result == null) {
            throw ChatExceptions.chatParticipantNotFound(chatRoomId, memberId); // ✅ 수정
        }

        return result;
    }

    /**
     * 채팅방 참여자 목록 조회 (MyBatis 사용)
     */
    public List<ChatParticipantsResponseDto> getParticipantsByRoomId(Long chatRoomId) {
        log.info("채팅방 참여자 목록 조회: chatRoomId={}", chatRoomId);

        return chatParticipantsMapper.findParticipantsByRoomId(chatRoomId);
    }

    /**
     * 내가 참여한 채팅방 목록 조회 (MyBatis 사용)
     */
    public List<ChatParticipantsResponseDto> getMyChatRooms(Long memberId) {
        log.info("내 채팅방 목록 조회: memberId={}", memberId);

        return chatParticipantsMapper.findMyChatRooms(memberId);
    }
}
