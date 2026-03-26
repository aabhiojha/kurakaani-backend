package com.abhishekojha.kurakanimonolith.common.objectStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Operations {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.bucket}")
    private String bucket;

    // upload and return the key
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        log.info("Uploaded file to s3: {}", key);
        return key;
    }

    public void deleteFile(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
        log.info("Deleted file from s3: {}", key);
    }

    public String generatePublicUrl(String key, String bucket) {
        return endpoint + "/" + bucket + "/" + key;
    }

    //    Generates a pre-signed URL valid for the given duration.
    public String getPresignedUrl(String key, Duration expiry) {
        String url = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(expiry)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build())
                        .build()
        ).url().toString();

        log.debug("Generated presigned URL for key: {}", key);
        return url;
    }
}
