package com.profect.tickle.global.smb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class NasSmbService {

    @Autowired
    private SmbSessionFactory smbSessionFactory;

    // 파일 업로드
    public void uploadFile(MultipartFile file, String remotePath) {
        try {
            SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(smbSessionFactory);

            // 임시 파일 생성
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFileName = "temp_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path tempFile = Paths.get(tempDir, tempFileName);

            // MultipartFile을 임시 파일로 저장
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Message 객체 생성하여 send 사용
            Map<String, Object> headers = new HashMap<>();
            headers.put(FileHeaders.FILENAME, remotePath);
            GenericMessage<File> message = new GenericMessage<>(tempFile.toFile(), headers);

            template.send(message);

            // 임시 파일 삭제
            Files.deleteIfExists(tempFile);

        } catch (Exception e) {
            throw new RuntimeException("SMB 파일 업로드 실패: " + e.getMessage(), e);
        }
    }

    // 바이트 배열로 파일 업로드
    public void uploadFile(byte[] fileData, String remotePath) {
        try {
            SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(smbSessionFactory);

            // 임시 파일 생성
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFileName = "temp_" + System.currentTimeMillis() + ".tmp";
            Path tempFile = Paths.get(tempDir, tempFileName);

            // 바이트 배열을 임시 파일로 저장
            Files.write(tempFile, fileData);

            // Message 객체 생성하여 send 사용
            Map<String, Object> headers = new HashMap<>();
            headers.put(FileHeaders.FILENAME, remotePath);
            GenericMessage<File> message = new GenericMessage<>(tempFile.toFile(), headers);

            template.send(message);

            // 임시 파일 삭제
            Files.deleteIfExists(tempFile);

        } catch (Exception e) {
            throw new RuntimeException("SMB 파일 업로드 실패: " + e.getMessage(), e);
        }
    }

    // 파일 다운로드
    public void downloadFile(String remotePath, String localFilePath) {
        try {
            SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(smbSessionFactory);

            template.get(remotePath, inputStream -> {
                try (FileOutputStream fos = new FileOutputStream(localFilePath);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException("SMB 파일 다운로드 실패: " + e.getMessage(), e);
        }
    }

    // InputStream으로 파일 다운로드
    public InputStream downloadFileAsStream(String remotePath) {
        try {
            SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(smbSessionFactory);
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFile = tempDir + File.separator + "temp_download_" + System.currentTimeMillis();

            template.get(remotePath, inputStream -> {
                try (FileOutputStream fos = new FileOutputStream(tempFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return new FileInputStream(tempFile);
        } catch (Exception e) {
            throw new RuntimeException("SMB 파일 다운로드 실패: " + e.getMessage(), e);
        }
    }

    // 파일 존재 여부 확인
    public boolean fileExists(String remotePath) {
        try {
            SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(smbSessionFactory);
            return template.exists(remotePath);
        } catch (Exception e) {
            return false;
        }
    }

    // 파일 삭제
    public boolean deleteFile(String remotePath) {
        try {
            SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(smbSessionFactory);
            return template.remove(remotePath);
        } catch (Exception e) {
            return false;
        }
    }

    // execute를 사용한 직접 제어 방식
    public void uploadFileWithExecute(MultipartFile file, String remotePath) {
        try {
            SmbRemoteFileTemplate template = new SmbRemoteFileTemplate(smbSessionFactory);

            template.execute(session -> {
                try {
                    session.write(file.getInputStream(), remotePath);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException("SMB 파일 업로드 실패: " + e.getMessage(), e);
        }
    }
}
