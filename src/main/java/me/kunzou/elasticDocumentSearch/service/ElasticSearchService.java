package me.kunzou.elasticDocumentSearch.service;

import me.kunzou.elasticDocumentSearch.dto.Document;
import me.kunzou.elasticDocumentSearch.dto.SearchResult;
import org.elasticsearch.action.index.IndexResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ElasticSearchService {
  Document addDataByMap(MultipartFile file) throws IOException;
  List<Document> getAllDocuments() throws IOException;
  void delete(String id) throws IOException;
  List<SearchResult> searchData(String keyword) throws IOException;
  Document getDocument(String id) throws IOException;
}
