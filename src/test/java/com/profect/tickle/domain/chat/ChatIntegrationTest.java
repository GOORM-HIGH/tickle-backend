package com.profect.tickle.domain.chat;

import com.profect.tickle.domain.chat.dto.request.ChatRoomJoinRequestDto;
import com.profect.tickle.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.request.ReadMessageRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatRoomResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatParticipantsResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.service.ChatRoomService;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import com.profect.tickle.domain.chat.service.ChatParticipantsService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.entity.Genre;
import com.profect.tickle.domain.performance.entity.Hall;
import com.profect.tickle.domain.performance.entity.HallType;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.performance.repository.GenreRepository;
import com.profect.tickle.domain.performance.repository.HallRepository;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 채팅 기능 통합 테스트 (Service 계층 직접 호출)
 * 
 * 테스트 범위:
 * - 채팅방 생성 → 참여자 추가 → 메시지 전송의 전체 플로우
 * - 여러 사용자가 동시에 채팅하는 시나리오
 * - 채팅방 상태 변경 및 참여자 관리
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("채팅 기능 통합 테스트 (Service 계층)")
class ChatIntegrationTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatParticipantsService chatParticipantsService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private StatusRepository statusRepository;

    private Member testMember1;
    private Member testMember2;
    private Performance testPerformance;
    private Long testChatRoomId;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testMember1 = Member.builder()
                .email("test1@example.com")
                .nickname("테스트사용자1")
                .phoneNumber("01012345678")
                .password("encodedPassword123")
                .memberRole(MemberRole.MEMBER)
                .build();
        testMember1 = memberRepository.save(testMember1);

        testMember2 = Member.builder()
                .email("test2@example.com")
                .nickname("테스트사용자2")
                .phoneNumber("01087654321")
                .password("encodedPassword456")
                .memberRole(MemberRole.MEMBER)
                .build();
        testMember2 = memberRepository.save(testMember2);

        // 테스트용 장르 생성
        Genre testGenre = Genre.builder()
                .title("테스트장르")
                .build();
        testGenre = genreRepository.save(testGenre);

        // 테스트용 공연장 생성
        Hall testHall = Hall.builder()
                .type(HallType.A)
                .address("테스트주소")
                .build();
        testHall = hallRepository.save(testHall);

        // 기존 상태 조회 (첫 번째 상태 사용)
        Status testStatus = statusRepository.findAll().get(0);

        // 테스트용 공연 생성
        testPerformance = Performance.builder()
                .title("테스트공연")
                .member(testMember1)
                .genre(testGenre)
                .hall(testHall)
                .status(testStatus)
                .date(Instant.now().plusSeconds(86400)) // 내일
                .runtime((short) 120)
                .price("30000")
                .img("test.jpg")
                .startDate(Instant.now().plusSeconds(86400))
                .endDate(Instant.now().plusSeconds(90000))
                .isEvent(false)
                .lookCount((short) 0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        testPerformance = performanceRepository.save(testPerformance);
    }

    @Test
    @DisplayName("TC-INTEGRATION-001: 채팅방 생성 → 참여자 추가 → 메시지 전송 전체 플로우")
    void shouldCompleteFullChatFlow() throws Exception {
        // 1단계: 채팅방 생성
        ChatRoomCreateRequestDto createRequest = new ChatRoomCreateRequestDto();
        createRequest.setPerformanceId(testPerformance.getId());
        createRequest.setRoomName("통합테스트 채팅방");
        createRequest.setMaxParticipants((short) 10);

        ChatRoomResponseDto chatRoomResponse = chatRoomService.createChatRoom(createRequest);
        assertNotNull(chatRoomResponse);
        assertEquals("통합테스트 채팅방", chatRoomResponse.getName());
        assertEquals((short) 10, chatRoomResponse.getMaxParticipants());
        
        testChatRoomId = chatRoomResponse.getChatRoomId();

        // 2단계: 참여자 추가 (testMember1)
        ChatRoomJoinRequestDto joinRequest = new ChatRoomJoinRequestDto();
        ChatParticipantsResponseDto participant1 = chatParticipantsService.joinChatRoom(testChatRoomId, testMember1.getId(), joinRequest);
        assertNotNull(participant1);
        assertEquals(testMember1.getId(), participant1.getMemberId());
        assertEquals(testChatRoomId, participant1.getChatRoomId());

        // 3단계: 메시지 전송
        ChatMessageSendRequestDto messageRequest = new ChatMessageSendRequestDto();
        messageRequest.setContent("안녕하세요! 통합테스트 메시지입니다.");

        ChatMessageResponseDto sentMessage = chatMessageService.sendMessage(testChatRoomId, testMember1.getId(), messageRequest);
        assertNotNull(sentMessage);
        assertEquals("안녕하세요! 통합테스트 메시지입니다.", sentMessage.getContent());
        assertEquals(testMember1.getId(), sentMessage.getMemberId());
    }

    @Test
    @DisplayName("TC-INTEGRATION-002: 여러 사용자가 동시에 채팅하는 시나리오")
    void shouldHandleMultipleUsersChattingSimultaneously() throws Exception {
        // 채팅방 생성
        ChatRoomCreateRequestDto createRequest = new ChatRoomCreateRequestDto();
        createRequest.setPerformanceId(testPerformance.getId());
        createRequest.setRoomName("동시채팅 테스트방");
        createRequest.setMaxParticipants((short) 5);

        ChatRoomResponseDto chatRoomResponse = chatRoomService.createChatRoom(createRequest);
        assertNotNull(chatRoomResponse);
        testChatRoomId = chatRoomResponse.getChatRoomId();

        // testMember1 참여
        ChatRoomJoinRequestDto joinRequest1 = new ChatRoomJoinRequestDto();
        ChatParticipantsResponseDto participant1 = chatParticipantsService.joinChatRoom(testChatRoomId, testMember1.getId(), joinRequest1);
        assertNotNull(participant1);

        // testMember2 참여
        ChatRoomJoinRequestDto joinRequest2 = new ChatRoomJoinRequestDto();
        ChatParticipantsResponseDto participant2 = chatParticipantsService.joinChatRoom(testChatRoomId, testMember2.getId(), joinRequest2);
        assertNotNull(participant2);

        // testMember1이 메시지 전송
        ChatMessageSendRequestDto message1 = new ChatMessageSendRequestDto();
        message1.setContent("안녕하세요! testMember1입니다.");

        ChatMessageResponseDto sentMessage1 = chatMessageService.sendMessage(testChatRoomId, testMember1.getId(), message1);
        assertNotNull(sentMessage1);
        assertEquals("안녕하세요! testMember1입니다.", sentMessage1.getContent());

        // testMember2가 메시지 전송
        ChatMessageSendRequestDto message2 = new ChatMessageSendRequestDto();
        message2.setContent("안녕하세요! testMember2입니다.");

        ChatMessageResponseDto sentMessage2 = chatMessageService.sendMessage(testChatRoomId, testMember2.getId(), message2);
        assertNotNull(sentMessage2);
        assertEquals("안녕하세요! testMember2입니다.", sentMessage2.getContent());

        // 참여자 목록 조회
        List<ChatParticipantsResponseDto> participants = chatParticipantsService.getParticipantsByRoomId(testChatRoomId);
        assertEquals(2, participants.size());
    }

    @Test
    @DisplayName("TC-INTEGRATION-003: 채팅방 상태 변경 및 참여자 관리")
    void shouldManageChatRoomStatusAndParticipants() throws Exception {
        // 채팅방 생성
        ChatRoomCreateRequestDto createRequest = new ChatRoomCreateRequestDto();
        createRequest.setPerformanceId(testPerformance.getId());
        createRequest.setRoomName("상태관리 테스트방");
        createRequest.setMaxParticipants((short) 3);

        ChatRoomResponseDto chatRoomResponse = chatRoomService.createChatRoom(createRequest);
        assertNotNull(chatRoomResponse);
        testChatRoomId = chatRoomResponse.getChatRoomId();

        // 채팅방 상태 변경 (비활성화)
        chatRoomService.updateChatRoomStatus(testChatRoomId, false);
        
        // 상태 변경 확인
        ChatRoomResponseDto updatedRoom = chatRoomService.getChatRoomById(testChatRoomId);
        assertFalse(updatedRoom.getStatus());

        // testMember1 참여
        ChatRoomJoinRequestDto joinRequest = new ChatRoomJoinRequestDto();
        ChatParticipantsResponseDto participant1 = chatParticipantsService.joinChatRoom(testChatRoomId, testMember1.getId(), joinRequest);
        assertNotNull(participant1);

        // testMember1이 채팅방 나가기
        chatParticipantsService.leaveChatRoom(testChatRoomId, testMember1.getId());
        
        // 나가기 확인
        List<ChatParticipantsResponseDto> remainingParticipants = chatParticipantsService.getParticipantsByRoomId(testChatRoomId);
        assertEquals(0, remainingParticipants.size());
    }

    @Test
    @DisplayName("TC-INTEGRATION-004: 내 채팅방 목록 조회")
    void shouldListMyChatRooms() throws Exception {
        // 채팅방 생성
        ChatRoomCreateRequestDto createRequest = new ChatRoomCreateRequestDto();
        createRequest.setPerformanceId(testPerformance.getId());
        createRequest.setRoomName("목록조회 테스트방");
        createRequest.setMaxParticipants((short) 10);

        ChatRoomResponseDto chatRoomResponse = chatRoomService.createChatRoom(createRequest);
        assertNotNull(chatRoomResponse);

        // 내 채팅방 목록 조회
        List<ChatParticipantsResponseDto> myRooms = chatParticipantsService.getMyChatRooms(testMember1.getId());
        // 새로 생성한 채팅방이 아직 참여하지 않았으므로 빈 목록이어야 함
        assertEquals(0, myRooms.size());
    }
}
