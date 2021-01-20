package me.kunzou.elasticDocumentSearch.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ElasticSearchResult {
  private String id;
  private String filename;
  private LocalDateTime createTime;
  private Attachment attachment;
  private Float score;
  private List<String> highlightFields;

  public List<String> getHighlightFields() {
    if(highlightFields == null) {
      highlightFields = new ArrayList<>();
    }
    return highlightFields;
  }
}
