package com.profect.tickle.global.s3.service;

import com.profect.tickle.global.s3.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * 파일 업로드 (기본 - 채팅용)
     */
    public void uploadFile(String key, InputStream inputStream, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));
            log.info("S3 파일 업로드 완료: {}", key);

        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: key={}, error={}", key, e.getMessage());
            throw new RuntimeException("S3 파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 업로드 (사용자별 분류 - 프로필/공연 이미지용)
     */
    public void uploadFile(String key, InputStream inputStream, String contentType, Long userId) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));
            log.info("S3 사용자별 파일 업로드 완료: key={}, userId={}", key, userId);

        } catch (Exception e) {
            log.error("S3 사용자별 파일 업로드 실패: key={}, userId={}, error={}", key, userId, e.getMessage());
            throw new RuntimeException("S3 파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 업로드
     */
    public void uploadPerformanceImage(String key, InputStream inputStream, String contentType, Long performanceId) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));
            log.info("S3 공연 이미지 업로드 완료: key={}, performanceId={}", key, performanceId);

        } catch (Exception e) {
            log.error("S3 공연 이미지 업로드 실패: key={}, performanceId={}, error={}", key, performanceId, e.getMessage());
            throw new RuntimeException("S3 공연 이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 다운로드 URL 생성 (PreSigned URL)
     */
    public String generatePreSignedUrl(String key) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.getPreSignedUrlExpiration()))
                .getObjectRequest(GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build())
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            
            log.info("S3 PreSigned URL 생성: key={}", key);
            return url;

        } catch (Exception e) {
            log.error("S3 PreSigned URL 생성 실패: key={}, error={}", key, e.getMessage());
            throw new RuntimeException("PreSigned URL 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 존재 확인
     */
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("파일 존재 확인 중 오류: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: key={}, error={}", key, e.getMessage());
            throw new RuntimeException("S3 파일 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * S3 Key 생성 (채팅 파일용)
     */
    public String buildChatFileKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "chat/" + datePath + "/" + fileName;
    }

    /**
     * S3 Key 생성 (채팅방별 파일용)
     */
    public String buildChatFileKey(String fileName, Long chatRoomId) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "chat/" + chatRoomId + "/" + datePath + "/" + fileName;
    }

    /**
     * S3 Key 생성 (사용자 파일용)
     */
    public String buildUserFileKey(String fileName, Long userId, String fileType) {
        return "users/" + userId + "/" + fileType + "/" + fileName;
    }

    /**
     * S3 Key 생성 (공연 이미지용)
     */
    public String buildPerformanceImageKey(String fileName, Long performanceId) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "performance/" + performanceId + "/images/" + datePath + "/" + fileName;
    }

    /**
     * S3 연결 테스트
     */
    public void testConnection() {
        try {
            log.info("=== S3 연결 테스트 시작 ===");
            log.info("Bucket: {}", s3Properties.getBucketName());
            log.info("Region: {}", s3Properties.getRegion());

            // 버킷 존재 확인
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(s3Properties.getBucketName())
                .build();

            s3Client.headBucket(headBucketRequest);
            log.info("S3 연결 성공!");

        } catch (Exception e) {
            log.error("S3 연결 실패: {}", e.getMessage());
            throw new RuntimeException("S3 연결에 실패했습니다: " + e.getMessage());
        }
    }
}
