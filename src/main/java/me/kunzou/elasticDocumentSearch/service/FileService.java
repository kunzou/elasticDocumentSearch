package me.kunzou.elasticDocumentSearch.service;

import me.kunzou.elasticDocumentSearch.dto.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {
  void writeFile(MultipartFile file, String id) throws IOException;
  byte[] download(Document document) throws IOException;
}
