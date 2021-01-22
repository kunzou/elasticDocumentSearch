package me.kunzou.elasticDocumentSearch.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ElasticSearchResult {
  private String id;
  private String filename;
  private LocalDateTime createTime;
  private Attachment attachment;
  private Float score;
  private List<String> highlightFields;
  private Map<String, List<String>> highlightFieldsMap;

  public List<String> getHighlightFields() {
    if(highlightFields == null) {
      highlightFields = new ArrayList<>();
    }
    return highlightFields;
  }



  public Map<String, List<String>> getHighlightFieldsMap() {
    if(highlightFieldsMap == null) {
      highlightFieldsMap = new HashMap<>();
    }
    return highlightFieldsMap;
  }
}
