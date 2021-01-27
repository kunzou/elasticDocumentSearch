package me.kunzou.elasticDocumentSearch.service;

import me.kunzou.elasticDocumentSearch.dto.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FileServiceImpl implements FileService {

  @Value("${config.filePath}") private String uploadPath;

  @Override
  public void writeFile(MultipartFile file, String id) throws IOException {
    Path path = Paths.get(uploadPath)
      .toAbsolutePath();
    Files.createDirectories(Paths.get(String.format("%s/%s", path.toString(), id)));
    Path filePath = Paths.get(String.format("%s/%s/%s", uploadPath, id, file.getOriginalFilename()));
    Files.write(filePath, file.getBytes());
  }

  @Override
  public byte[] download(Document document) throws IOException {
    Path filePath = Paths.get(String.format("%s/%s", uploadPath, document.getId()));
    Path path = Stream.of(Objects.requireNonNull(new File(filePath.toString()).listFiles()))
      .findAny()
      .map(File::toPath)
      .orElseThrow(()->new IOException("File not found"));

    return Files.readAllBytes(path);
  }
}
