package me.kunzou.elasticDocumentSearch.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Document {
  private String id;
  private String fileName;
  private String fileType;
  private String content;
  private String title;
  private String size;
  private List<String> highlights;
  private Float score;
  private String author;
}
