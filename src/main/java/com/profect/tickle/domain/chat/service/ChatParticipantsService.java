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

        // 1. 채팅방과 회원 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> ChatExceptions.chatRoomNotFound(chatRoomId)); // ✅ 수정

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ChatExceptions.memberNotFoundInChat(memberId)); // ✅ 수정

        // 2. 이미 참여 중인지 확인
        Optional<ChatParticipants> existingParticipant =
                chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member);

        ChatParticipants participant;

        if (existingParticipant.isPresent()) {
            // 이미 참여했던 경우 → 상태만 활성화
            participant = existingParticipant.get();
            if (participant.getStatus()) {
                throw ChatExceptions.chatAlreadyParticipant(chatRoomId); // ✅ 수정
            }
            participant.rejoin();
        } else {
            // 3. 참여자 수 확인
            int currentParticipants = chatParticipantsRepository.countByChatRoomAndStatusTrue(chatRoom);
            if (!chatRoom.canJoin(currentParticipants)) {
                throw ChatExceptions.chatRoomCapacityExceeded(); // ✅ 수정
            }

            // 4. 새 참여자 생성
            participant = ChatParticipants.builder()
                    .chatRoom(chatRoom)
                    .member(member)
                    .joinedAt(Instant.now())
                    .status(true)
                    .build();
        }

        ChatParticipants savedParticipant = chatParticipantsRepository.save(participant);
        log.info("채팅방 참여 완료: participantId={}", savedParticipant.getId());

        return ChatParticipantsResponseDto.fromEntity(savedParticipant);
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
