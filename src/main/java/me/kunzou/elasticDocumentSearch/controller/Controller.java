package me.kunzou.elasticDocumentSearch.controller;

import me.kunzou.elasticDocumentSearch.dto.Document;
import me.kunzou.elasticDocumentSearch.dto.SearchResult;
import me.kunzou.elasticDocumentSearch.service.ElasticSearchService;
import me.kunzou.elasticDocumentSearch.service.FileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class Controller {

  private ElasticSearchService elasticSearchService;
  private FileService fileService;

  public Controller(ElasticSearchService elasticSearchService, FileService fileService) {
    this.elasticSearchService = elasticSearchService;
    this.fileService = fileService;
  }

  @PostMapping(value = "/upload")
  public ResponseEntity<Document> attachmentUpload(@RequestParam("file") MultipartFile file) throws Exception {
    if (file.isEmpty()) {
      return new ResponseEntity(HttpStatus.BAD_REQUEST);
    }

    Document document = elasticSearchService.addDataByMap(file);
    fileService.writeFile(file, document.getId());
    return ResponseEntity.ok().body(elasticSearchService.addDataByMap(file));
  }

  @GetMapping(value = "/search/{keyword}")
  public ResponseEntity<List<SearchResult>> search(@PathVariable("keyword") String keyword) throws IOException {
    return ResponseEntity.ok().body(elasticSearchService.searchData(keyword));
  }

  @GetMapping(value = "/documents")
  public ResponseEntity<List<Document>> getAllDocuments() throws IOException {
    return ResponseEntity.ok().body(elasticSearchService.getAllDocuments());
  }

  @DeleteMapping(value = "/delete/{id}")
  public ResponseEntity deleteDocument(@PathVariable("id") String id) throws IOException {
    elasticSearchService.delete(id);
    return new ResponseEntity(HttpStatus.OK);
  }

  @GetMapping(value = "/download/{id}")
  public ResponseEntity<byte[]> getAllDocuments(@PathVariable("id") String id) throws IOException {
    Document document = elasticSearchService.getDocument(id);
    byte[] bytes = fileService.download(document);

    HttpHeaders respHeaders = new HttpHeaders();
    respHeaders.setContentType(new MediaType("text", "json"));
    respHeaders.setCacheControl("must-revalidate, post-check=0, pre-check=0");
    respHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + document.getFileName());

    return new ResponseEntity<>(bytes, respHeaders, HttpStatus.OK);
  }

}
