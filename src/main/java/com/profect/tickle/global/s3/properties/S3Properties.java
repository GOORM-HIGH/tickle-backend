package com.profect.tickle.global.s3.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3Properties {
    private String bucketName;
    private String region;
    private String accessKey;
    private String secretKey;
    private String baseUrl;
    private int preSignedUrlExpiration = 3600; // 1시간 (기본값)
}
