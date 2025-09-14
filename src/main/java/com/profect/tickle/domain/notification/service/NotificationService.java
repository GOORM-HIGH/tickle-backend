package com.profect.tickle.domain.notification.service;

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
import com.profect.tickle.global.util.PgCopyBinaryUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.copy.PGCopyOutputStream;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.DataOutputStream;
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
    public long saveAll(
            List<Long> memberIdList,
            Long templateId,
            String subject,
            String content,
            Instant createdAt
    ) throws Exception {
        log.info("알림 전체 저장 시작");
        long startTime = System.currentTimeMillis();

        if (memberIdList == null || memberIdList.isEmpty()) return 0L;

        final long statusId = StatusIds.Notification.UNREAD;
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            PGConnection pgConn = conn.unwrap(PGConnection.class);

            final String copySql =
                    "COPY notification (" +
                            "  notification_received_member_id," +
                            "  notification_template_id," +
                            "  notification_title," +
                            "  notification_content," +
                            "  notification_created_at," +
                            "  status_id" +
                            ") FROM STDIN WITH (FORMAT binary)";

            try (PGCopyOutputStream pgOut = new PGCopyOutputStream(pgConn, copySql);
                 DataOutputStream out = new DataOutputStream(pgOut)) {

                PgCopyBinaryUtils.writeHeader(out);

                final int COLS = 6;
                for (Long id : memberIdList) {
                    out.writeShort(COLS);           // number of columns (int16)

                    PgCopyBinaryUtils.writeInt8(out, id);          // BIGINT
                    PgCopyBinaryUtils.writeInt8(out, templateId);         // BIGINT
                    PgCopyBinaryUtils.writeText(out, subject);            // TEXT
                    PgCopyBinaryUtils.writeText(out, content);            // TEXT
                    PgCopyBinaryUtils.writeTimestamptz(out, createdAt);   // TIMESTAMPTZ
                    PgCopyBinaryUtils.writeInt8(out, statusId);           // BIGINT
                }

                PgCopyBinaryUtils.writeTrailer(out);
                out.flush();
            }

            long endTime = System.currentTimeMillis();
            log.info("알림 전체 저장 종료. 소요시간: {} ms", endTime - startTime);
            return memberIdList.size();
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }
}
