package ru.ulstu.report.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import io.minio.errors.ErrorResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
public void init() {
    try {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Bucket {} создан при старте", bucketName);
        } else {
            log.info("Bucket {} уже существует", bucketName);
        }
    } catch (Exception e) {
        log.error("Не удалось проверить/создать bucket {}", bucketName, e);
    }
}


    // Метод для загрузки файла
    public String uploadFile(String objectName, byte[] content) throws Exception {
        // Создаем бакет, если его нет
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // Загружаем файл
        try (InputStream is = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, content.length, -1)
                            .contentType("text/csv")
                            .build()
            );
        }
        log.info("Файл {} успешно загружен в MinIO", objectName);
        return objectName;
    }

    // Метод для получения файла
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    public boolean objectExists(String objectName) throws Exception {
    try {
        minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        return true;
    } catch (ErrorResponseException e) {
        String code = e.errorResponse().code();
        if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
            return false;
        }
        throw e;
    }
}

public void removeFile(String objectName) throws Exception {
    minioClient.removeObject(
            RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
    );
    log.info("Файл {} удалён из MinIO", objectName);
}

}