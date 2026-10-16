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

import io.minio.errors.ErrorResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    @Value("${minio.bucket-name}")
    private String bucketName;

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
        if ("NoSuchKey".equals(e.errorResponse().code())) {
            return false;
        }
        throw e;
    }
}

}