package me.kunzou.elasticDocumentSearch.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class SearchResult {
  private String fileName;
  private List<String> contentHighlights;
  private List<String> titleHighlights;
  private List<String> keywordsHighlights;
  private Float score;

  public List<String> getContentHighlights() {
    if(contentHighlights == null) {
      contentHighlights = new ArrayList<>();
    }
    return contentHighlights;
  }

  public List<String> getTitleHighlights() {
    if(titleHighlights == null) {
      titleHighlights = new ArrayList<>();
    }
    return titleHighlights;
  }

  public List<String> getKeywordsHighlights() {
    if(keywordsHighlights == null) {
      keywordsHighlights = new ArrayList<>();
    }
    return keywordsHighlights;
  }
}
