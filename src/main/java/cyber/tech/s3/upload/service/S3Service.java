package cyber.tech.s3.upload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class S3Service {

  private final S3Client s3Client;

  @Value("${app.upload.bucket-name}")
  private String bucketName;

  public S3Service(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  public String uploadImage(MultipartFile file) throws IOException {
    var fileName = generateFileName(file.getOriginalFilename());

    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileName)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build(),
        RequestBody.fromBytes(file.getBytes()));

    return fileName;
  }

  public byte[] downloadImage(String fileName) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(fileName).build();

    return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
  }

  public List<String> listImages() {
    ListObjectsV2Request listObjectsRequest =
        ListObjectsV2Request.builder().bucket(bucketName).build();

    ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

    return listObjectsResponse.contents().stream()
        .map(S3Object::key)
        .filter(key -> !key.endsWith("/"))
        .toList();
  }

  public void deleteImage(String fileName) {
    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(bucketName).key(fileName).build();

    s3Client.deleteObject(deleteObjectRequest);
  }

  public String getImageUrl(String fileName) {
    return String.format(
        "%s/%s/%s",
        System.getenv("LOCALSTACK_ENDPOINT") != null
            ? System.getenv("LOCALSTACK_ENDPOINT")
            : "http://localhost:4566",
        bucketName,
        fileName);
  }

  private String generateFileName(String originalFileName) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String uuid = UUID.randomUUID().toString().substring(0, 8);

    String extension = "";
    if (originalFileName != null && originalFileName.contains(".")) {
      extension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }

    return String.format("image_%s_%s%s", timestamp, uuid, extension);
  }
}
