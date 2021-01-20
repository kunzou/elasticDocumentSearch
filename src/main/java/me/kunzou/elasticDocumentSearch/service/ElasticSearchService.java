package me.kunzou.elasticDocumentSearch.service;

import me.kunzou.elasticDocumentSearch.dto.Document;
import org.elasticsearch.action.index.IndexResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ElasticSearchService {
  IndexResponse addDataByMap(MultipartFile file) throws IOException;
  List<Document> searchData(String keyword, boolean fuzzy) throws IOException;
  List<Document> getAllDocuments() throws IOException;
  void delete(String id) throws IOException;
}
