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
import org.springframework.util.StopWatch;

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
            List<Long> memberIdList,
            Long templateId,
            String subject,
            String content,
            Instant createdAt
    ) throws Exception {
        if (memberIdList == null || memberIdList.isEmpty()) return 0L;

        StopWatch sw = new StopWatch("saveAllWithCopy");
        sw.start("csv-build");

        long statusId = StatusIds.Notification.UNREAD;

        // 1) 상수 필드는 한 번만 이스케이프해서 캐시(반복 이스케이프 제거)
        String escTemplateId = CsvEscaper.escape(templateId);
        String escSubject    = CsvEscaper.escape(subject);
        String escContent    = CsvEscaper.escape(content);
        String escCreatedAt  = CsvEscaper.escape(createdAt);
        String escStatusId   = CsvEscaper.escape(statusId);

        final int avgRow = 64 + (subject != null ? subject.length() : 0)
                + (content != null ? content.length() : 0);
        StringBuilder sb = new StringBuilder(Math.max(8 * 1024, memberIdList.size() * avgRow));

        for (Long id : memberIdList) {
            // memberId(숫자)는 그 자체로 안전 → 바로 append
            sb.append(id).append(',')
                    .append(escTemplateId).append(',')
                    .append(escSubject).append(',')
                    .append(escContent).append(',')
                    .append(escCreatedAt).append(',')
                    .append(escStatusId).append('\n');
        }
        sw.stop();

        sw.start("toString");
        String csv = sb.toString();   // 대용량 복제
        sw.stop();

        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            PGConnection pgConn = conn.unwrap(PGConnection.class);
            CopyManager copyManager = pgConn.getCopyAPI();

            final String copySql =
                    "COPY notification (" +
                            "  notification_received_member_id, " +
                            "  notification_template_id, " +
                            "  notification_title, " +
                            "  notification_content, " +
                            "  notification_created_at, " +
                            "  status_id" +
                            ") FROM STDIN WITH (FORMAT csv, DELIMITER ',', QUOTE '\"', ESCAPE '\"', NULL '')";

            sw.start("copyIn");
            long rows;
            try (Reader reader = new StringReader(csv)) {
                rows = copyManager.copyIn(copySql, reader);
            }
            sw.stop();

            log.info("\n{}", sw.prettyPrint());  // 각 구간 소요시간 로그로 확인
            return rows;
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /** 상수 필드용 간단 이스케이퍼 */
    static final class CsvEscaper {
        static String escape(Object v) {
            if (v == null) return ""; // COPY NULL ''에 맞춤
            String s = (v instanceof Instant i) ? i.toString() : String.valueOf(v);
            boolean q = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
            if (!q) return s;
            StringBuilder b = new StringBuilder(s.length() + 8).append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') b.append("\"\"");
                else b.append(c);
            }
            return b.append('"').toString();
        }
    }
}
