package com.profect.tickle.domain.performance.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.mapper.MemberMapper;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.event.performance.event.PerformanceModifiedEvent;
import com.profect.tickle.domain.performance.dto.request.PerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.request.UpdatePerformanceRequestDto;
import com.profect.tickle.domain.performance.dto.response.*;
import com.profect.tickle.domain.performance.entity.Genre;
import com.profect.tickle.domain.performance.entity.Hall;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.performance.repository.GenreRepository;
import com.profect.tickle.domain.performance.repository.HallRepository;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import com.profect.tickle.domain.reservation.mapper.ReservationMapper;
import com.profect.tickle.domain.reservation.repository.SeatTemplateRepository;
import com.profect.tickle.domain.reservation.service.SeatService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.Cursor;
import com.profect.tickle.global.paging.CursorPageResponse;
import com.profect.tickle.global.paging.PageRequest;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.s3.service.S3Service;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PerformanceService {

    private final ApplicationEventPublisher eventPublisher;

    private final PerformanceRepository performanceRepository;
    private final MemberRepository memberRepository;
    private final GenreRepository genreRepository;
    private final HallRepository hallRepository;
    private final StatusRepository statusRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final SeatService seatService;
    private final PerformanceMapper performanceMapper;
    private final MemberMapper memberMapper;
    private final ReservationMapper reservationMapper;
    private final S3Service s3Service;

    public List<GenreDto> getAllGenre() {
        return performanceMapper.findAllGenres();
    }

    public CursorPageResponse<PerformanceDto> findPerformancesByGenreCursor(Long genreId, Cursor cursor, int limit) {
        // limit + 1 조회해서 다음 페이지 존재 여부 확인
        List<PerformanceDto> performances = performanceMapper.findPerformancesByGenreCursor(genreId, cursor, limit + 1);

        boolean hasNext = performances.size() > limit;
        if (hasNext) {
            performances.remove(performances.size() - 1);
        }

        Cursor nextCursor = null;
        if (!performances.isEmpty()) {
            PerformanceDto last = performances.get(performances.size() - 1);
            nextCursor = new Cursor(last.getDate(), last.getPerformanceId());
        }

        return new CursorPageResponse<>(performances, nextCursor, hasNext);
    }

    public List<PerformanceDto> getTop10ByGenre(Long genreId) {
        validateGenreId(genreId);
        return performanceMapper.findTop10ByGenre(genreId);
    }

    public List<PerformanceDto> getTop10Performances() {
        return performanceMapper.findTop10ByClickCount();
    }

    @Transactional
    public PerformanceDetailDto getPerformanceDetail(Long performanceId) {
        validatePerfId(performanceId);

        PerformanceDetailDto result = performanceMapper.findDetailById(performanceId);
            if (result == null) {
                throw new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND);
        }

        performanceMapper.increaseLookCount(performanceId);

        return result;
    }

    public List<PerformanceDto> getTop4UpcomingPerformances() {
        LocalDateTime now =  LocalDateTime.now();
        return performanceMapper.findTop4UpcomingPerformances(now);
    }

    public CursorPageResponse<PerformanceDto> searchByKeyword(
            String keyword, int size,
            Instant cursorDate, Long cursorId
    ) {
        int pageSize = Math.min(Math.max(size, 1), 100);

        // LIMIT + 1 전략으로 hasNext 확인 (count 호출 제거)
        List<PerformanceDto> rows = performanceMapper.searchPerformancesByKeyword(
                keyword, pageSize + 1, cursorDate, cursorId
        );

        boolean hasNext = rows.size() > pageSize;
        List<PerformanceDto> items = hasNext ? rows.subList(0, pageSize) : rows;

        Cursor next = null;
        if (hasNext) {
            PerformanceDto last = items.get(items.size() - 1);
            next = new Cursor(last.getDate(), last.getPerformanceId());
        }
        return new CursorPageResponse<>(items, next, hasNext);
    }

    // 필요할 때만 호출되는 정확 카운트 (캐시 권장)
    // @Cacheable(cacheNames = "perfSearchCount", key = "#keyword", unless = "#result > 100000")
    public long countByKeyword(String keyword) {
        return performanceMapper.countPerformancesByKeyword(keyword);
    }

    public List<PerformanceDto> getRelatedPerformances(Long performanceId) {
        validatePerfId(performanceId);

        Long genreId = performanceMapper.findGenreIdByPerformanceId(performanceId);
        if (genreId == null) {
            throw new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND);
        }

        return performanceMapper.findRelatedPerformances(genreId, performanceId);
    }

    @Transactional
    public PerformanceResponseDto createPerformance(PerformanceRequestDto dto) {
        Long signInMemberId = SecurityUtil.getSignInMemberId();

        Member member = memberRepository.findById(signInMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Genre genre = genreRepository.findById(dto.getGenreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GENRE_NOT_FOUND));

        Hall hall = hallRepository.findByTypeAndAddress(dto.getHallType(), dto.getHallAddress())
                .orElseGet(() -> {
                    try {
                        return hallRepository.save(Hall.builder()
                                .type(dto.getHallType())
                                .address(dto.getHallAddress().trim())
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        return hallRepository.findByTypeAndAddress(dto.getHallType(), dto.getHallAddress())
                                .orElseThrow(() -> e);
                    }
                });

        Status status = statusRepository.findById(1L)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_STATUS_NOT_FOUND));

        Integer minPrice = seatTemplateRepository.findMinPriceByHallType(dto.getHallType());
        Integer maxPrice = seatTemplateRepository.findMaxPriceByHallType(dto.getHallType());
        if (minPrice == null || maxPrice == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        String priceRange = minPrice + " ~ " + maxPrice;

        Performance performance = Performance.create(dto, member, genre, hall, status, priceRange);
        performanceRepository.save(performance);

        seatService.createSeatsForPerformance(performance.getId());

        return PerformanceResponseDto.from(performance);
    }

    @Transactional
    public PerformanceResponseDto updatePerformance(Long performanceId, UpdatePerformanceRequestDto dto) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        performance.updateFrom(dto);

        // 공연정보 변경 이벤트 생성: 알림을 보내기 위함
        publishPerformanceModifiedEvent(performance.getId());

        return PerformanceResponseDto.from(performance);
    }

    @Transactional
    public void deletePerformance(Long performanceId, Long memberId) {
        Performance performance = performanceRepository.findActiveById(performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        if (!performance.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        performance.markAsDeleted();
    }

    public PagingResponse<PerformanceHostDto> getMyPerformances(Long memberId, int page, int size) {
        // 권한/본인 확인 (기존 로직 유지)
        Long signInMemberId = SecurityUtil.getSignInMemberId();
        Member me = memberRepository.findById(signInMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (me.getMemberRole() != MemberRole.HOST || !signInMemberId.equals(memberId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // page/size 기본 검증
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        long total = performanceMapper.countPerformancesByMemberId(memberId);
        if (total == 0) {
            return PagingResponse.from(List.of(), page, size, 0L);
        }

        int offset = page * size;
        List<PerformanceHostDto> content =
                performanceMapper.findPerformancesByMemberIdPaged(memberId, offset, size);

        return PagingResponse.from(content, page, size, total);
    }

    // 알림 수정 이벤트 발생 메서드
    private void publishPerformanceModifiedEvent(Long performanceId) {
        // 1) 공연정보 조회
        PerformanceServiceDto performanceServiceDto = performanceMapper.findById(performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        // 2) 예매정보 조회
        List<ReservationServiceDto> reservationList = reservationMapper.findByPerformanceId(performanceId);

        if (reservationList == null || reservationList.isEmpty()) {
            return; // 알림 이벤트를 발생시킬 필요가 없다.
        }

        // 3) 예매별 자리 정보 조회
        for (ReservationServiceDto reservation : reservationList) {
            List<ReservedSeatDto> seatList = reservationMapper.findReservedSeatListByReservationId(reservation.getId());
            reservation.setSeatList(seatList);
        }

        // 4) 이벤트 발행
        eventPublisher.publishEvent(new PerformanceModifiedEvent(
                performanceServiceDto,
                reservationList)
        );
        log.info("[{} 이벤트 발행]", NotificationKind.PERFORMANCE_MODIFIED);
    }

    private void validatePerfId(Long performanceId) {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateGenreId(Long genreId) {
        if (genreId == null || genreId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private <T> PagingResponse<T> emptyResponse(PageRequest pr, long total) {
        return PagingResponse.from(List.of(), pr.page(), pr.size(), total);
    }

    /**
     * 공연 이미지 URL을 PreSigned URL로 변환하는 헬퍼 메서드
     * S3에 저장된 파일이면 PreSigned URL을 생성하고, 기존 URL이면 그대로 반환
     */
    private String convertToPreSignedUrl(String imgUrl) {
        if (imgUrl == null || imgUrl.isEmpty()) {
            return null;
        }
        
        // S3에 저장된 파일이면 PreSigned URL 생성
        if (imgUrl.startsWith("performance/") || imgUrl.startsWith("users/")) {
            try {
                return s3Service.generatePreSignedUrl(imgUrl);
            } catch (Exception e) {
                log.warn("PreSigned URL 생성 실패: {}", imgUrl, e);
                return null; // 기본 이미지 사용
            }
        }
        
        // 기존 URL이면 그대로 반환
        return imgUrl;
    }

    /**
     * PerformanceDto에 이미지 URL 변환을 적용하는 헬퍼 메서드
     */
    private PerformanceDto convertToDtoWithImage(Performance performance) {
        String imgUrl = convertToPreSignedUrl(performance.getImg());
        
        return PerformanceDto.builder()
                .performanceId(performance.getId())
                .title(performance.getTitle())
                .date(performance.getDate())
                .img(imgUrl)
                .build();
    }

    /**
     * PerformanceDetailDto에 이미지 URL 변환을 적용하는 헬퍼 메서드
     */
    private PerformanceDetailDto convertToDetailDtoWithImage(Performance performance) {
        String imgUrl = convertToPreSignedUrl(performance.getImg());
        
        return PerformanceDetailDto.builder()
                .performanceId(performance.getId())
                .title(performance.getTitle())
                .img(imgUrl)
                .date(performance.getDate())
                .statusDescription(performance.getStatus().getDescription())
                .runtime(performance.getRuntime())
                .isEvent(performance.getIsEvent())
                .price(performance.getPrice())
                .hallAddress(performance.getHall().getAddress())
                .hostBizName(performance.getMember().getHostBizName())
                .startDate(performance.getStartDate())
                .endDate(performance.getEndDate())
                .build();
    }

}
