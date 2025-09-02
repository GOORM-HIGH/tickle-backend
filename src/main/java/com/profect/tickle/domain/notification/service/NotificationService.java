package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import com.profect.tickle.global.util.CsvUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    // utils
    private final StatusProvider statusProvider;
    private final DataSource dataSource;

    // services
    private final MemberService memberService;

    // mappers & repositories
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    // 알림 조회 메서드
    public List<NotificationResponseDto> getNotificationListByMemberId(Long memberId, int limit) {
        return notificationMapper.getNotificationListByMemberId(memberId, limit);
    }

    // 신규 알림 저장 메서드
    @Transactional
    public Long saveNotification(
            @NotNull String toEmail,
            @NotNull NotificationTemplate template,
            @NotNull String title,
            @NotNull String content,
            @NotNull Instant createdAt
    ) {
        // 1) 수신자 조회 (없으면 내부에서 예외 발생하도록 설계 권장)
        Member receivedMember = memberService.getMemberByEmail(toEmail);

        // 2) 엔티티 생성
        Notification notification = Notification.builder()
                .receivedMember(receivedMember)
                .template(template)
                .title(title)
                .content(content)
                .status(statusProvider.provide(StatusIds.Notification.UNREAD))
                .createdAt(createdAt)
                .build();

        // 3) 저장 후 ID 반환
        return notificationRepository.save(notification).getId();
    }

    // 알림 읽음 처리 메서드
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        // 알림 조회한다
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 회원의 알림인지 확인한다.
        if (!notification.isForMember(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        // 읽음 처리한다.
        notification.markAsRead(statusProvider.provide(StatusIds.Notification.READ));
        log.info("{}님의 {}번 알림 읽음 처리 완료", memberId, notificationId);
    }

    @Transactional
    public void saveAll(
            List<MemberResponseDto> memberList,
            Long templateId,
            String subject,
            String content,
            Instant createdAt
    ) {
        if (memberList == null || memberList.isEmpty()) return;

        final int CHUNK = 5_000;
        for (int i = 0; i < memberList.size(); i += CHUNK) {
            int end = Math.min(i + CHUNK, memberList.size());
            List<MemberResponseDto> sub = memberList.subList(i, end);
            notificationMapper.saveAll(sub, templateId, subject, content, createdAt, StatusIds.Notification.UNREAD);
        }
    }

    @Transactional
    public long saveAllWithCopy(
            List<MemberResponseDto> memberList,
            Long templateId,
            String subject,
            String content,
            Instant createdAt
    ) throws Exception {
        if (memberList == null || memberList.isEmpty()) return 0L;

        long statusId = StatusIds.Notification.UNREAD;
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            PGConnection pgConn = conn.unwrap(PGConnection.class);
            CopyManager copyManager = pgConn.getCopyAPI();
            final int avgRow = 64 + (subject != null ? subject.length() : 0)
                    + (content != null ? content.length() : 0);
            StringBuilder sb = new StringBuilder(Math.max(8 * 1024, memberList.size() * avgRow));

            // 1) CSV 문자열 생성 (행 단위로 StringBuilder에 누적)
            for (MemberResponseDto m : memberList) {
                CsvUtils.appendCsvRow(sb, m.getId(), templateId, subject, content, createdAt, statusId);
            }
            final String csv = sb.toString();

            // 2) COPY … FROM STDIN (CSV 옵션 명시: NULL '', 구분자/따옴표/이스케이프)
            final String copySql =
                    "COPY notification (" +
                            "  notification_received_member_id, " +
                            "  notification_template_id, " +
                            "  notification_title, " +
                            "  notification_content, " +
                            "  notification_created_at, " +
                            "  status_id" +
                            ") FROM STDIN WITH (FORMAT csv, DELIMITER ',', QUOTE '\"', ESCAPE '\"', NULL '')";

            try (Reader reader = new StringReader(csv)) {
//                return copyManager.copyIn(copySql, reader);
                return 1L;
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }
}
