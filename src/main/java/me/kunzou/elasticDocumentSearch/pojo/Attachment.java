package me.kunzou.elasticDocumentSearch.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class Attachment {
  private String content;
  @JsonProperty("content_length")
  private Integer contentLength;
  @JsonProperty("content_type")
  private String contentType;
  private ZonedDateTime date;
  private String language;
  private String author;
  private String title;
  private String keywords;
}
