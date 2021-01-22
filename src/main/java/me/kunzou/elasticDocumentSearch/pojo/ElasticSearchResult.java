package me.kunzou.elasticDocumentSearch.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ElasticSearchResult {
  private String filename;
  private LocalDateTime createTime;
  private Attachment attachment;
}
