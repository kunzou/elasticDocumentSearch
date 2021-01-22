package me.kunzou.elasticDocumentSearch.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
public class Document {
  private String id;
  private String fileName;
  private String fileType;
  private String content;
  private String title;
  private String size;
  private String author;
}
