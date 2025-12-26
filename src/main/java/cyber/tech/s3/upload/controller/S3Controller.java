package cyber.tech.s3.upload.controller;

import cyber.tech.s3.upload.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class S3Controller {

  private final S3Service s3Service;

  @Autowired
  public S3Controller(S3Service s3Service) {
    this.s3Service = s3Service;
  }

  @PostMapping("/upload")
  public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
    try {
      String fileName = s3Service.uploadImage(file);

      Map<String, String> response = new HashMap<>();
      response.put("fileName", fileName);
      response.put("message", "Imagem enviada com sucesso");
      response.put("url", s3Service.getImageUrl(fileName));

      return ResponseEntity.ok(response);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Erro ao processar a imagem: " + e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/download/{fileName}")
  public ResponseEntity<ByteArrayResource> downloadImage(@PathVariable String fileName) {
    try {
      byte[] data = s3Service.downloadImage(fileName);
      ByteArrayResource resource = new ByteArrayResource(data);

      var contentType = determineContentType(fileName);

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
          .body(resource);
    } catch (Exception e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/list")
  public ResponseEntity<Map<String, Object>> listImages() {
    try {
      List<String> images = s3Service.listImages();

      Map<String, Object> response = new HashMap<>();
      response.put("images", images);
      response.put("count", images.size());
      response.put("bucket", "bucket-cy");

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Erro ao listar imagens: " + e.getMessage()));
    }
  }

  @DeleteMapping("/delete/{fileName}")
  public ResponseEntity<Map<String, String>> deleteImage(@PathVariable String fileName) {
    try {
      s3Service.deleteImage(fileName);
      return ResponseEntity.ok(Map.of("message", "Imagem deletada com sucesso"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Erro ao deletar imagem: " + e.getMessage()));
    }
  }

  @GetMapping("/{fileName}/url")
  public ResponseEntity<Map<String, String>> getImageUrl(@PathVariable String fileName) {
    try {
      String url = s3Service.getImageUrl(fileName);
      return ResponseEntity.ok(Map.of("url", url));
    } catch (Exception e) {
      return ResponseEntity.notFound().build();
    }
  }

  private String determineContentType(String fileName) {
    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
      return "image/jpeg";
    } else if (fileName.endsWith(".png")) {
      return "image/png";
    } else if (fileName.endsWith(".gif")) {
      return "image/gif";
    } else if (fileName.endsWith(".webp")) {
      return "image/webp";
    } else {
      return "application/octet-stream";
    }
  }
}
