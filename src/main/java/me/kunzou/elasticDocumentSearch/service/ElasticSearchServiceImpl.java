package me.kunzou.elasticDocumentSearch.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.kunzou.elasticDocumentSearch.dto.Document;
import me.kunzou.elasticDocumentSearch.pojo.ElasticSearchResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

  @Value("${config.elastic.ip}") private String ip;
  @Value("${config.elastic.port}") private int port;
  @Value("${config.index}") private String index;
  @Value("${config.type}") private String type;
  @Value("${config.pipelineId}") private String pipelineId;
  @Value("${config.pipelineField}")  private String pipelineField;
  @Value("${config.attachmentField}") private String attachmentField;

  private RestHighLevelClient client;

  public ElasticSearchServiceImpl(RestHighLevelClient client) {
    this.client = client;
  }
  @PostConstruct
  private void initIndex() throws IOException {
    if(!indexExists()) {
      createIndex();
    }

    if(!pipelineExists()) {
      createPipeline();
    }
  }

  private boolean indexExists() throws IOException {
    GetIndexRequest request = new GetIndexRequest(index);
    request.local(false);
    request.humanReadable(true);
    request.includeDefaults(false);

    return client.indices().exists(request, RequestOptions.DEFAULT);
  }

  private void createIndex() throws IOException {
    CreateIndexRequest request = new CreateIndexRequest(index);
    request.settings(Settings.builder()
      .put("index.number_of_shards", 3)
      .put("index.number_of_replicas", 2)
    );

    client.indices().create(request, RequestOptions.DEFAULT);
  }

  private boolean pipelineExists() throws IOException {
    GetPipelineRequest request = new GetPipelineRequest(pipelineId);
    return client.ingest().getPipeline(request, RequestOptions.DEFAULT).isFound();
  }

  private AcknowledgedResponse createPipeline() throws IOException {
    String source = "{" +
      " \"description\" : \"Extract attachment information\"," +
      " \"processors\":[" +
      " {" +
      "    \"attachment\":{" +
      "        \"field\":\"" + pipelineField + "\"," +
      "        \"indexed_chars\" : -1," +
      "        \"ignore_missing\":true" +
      "     }" +
      " }," +
      " {" +
      "     \"remove\":{\"field\":\"" + pipelineField + "\"}" +
      " }]}";

    PutPipelineRequest request = new PutPipelineRequest(
      pipelineId,
      new BytesArray(source.getBytes(StandardCharsets.UTF_8)),
      XContentType.JSON
    );

    return client.ingest().putPipeline(request, RequestOptions.DEFAULT);
  }

  @Override
  public IndexResponse addDataByMap(MultipartFile file) throws IOException {
    IndexRequest indexRequest = new IndexRequest(index);
    indexRequest.setPipeline(pipelineId);

    Map<String, Object> map = new HashMap();
    map.put(pipelineField, Base64.encodeBase64String(file.getBytes()));
    map.put("createTime", new Date());
    map.put("filename", file.getOriginalFilename());

    indexRequest.source(map);
    return client.index(indexRequest, RequestOptions.DEFAULT);
  }

  @Override
  public List<Document> searchData(String keyword, boolean fuzzy) throws IOException {
    SearchResponse response = searchByKeyword(keyword, fuzzy);

    SearchHits hits = response.getHits();
    SearchHit[] searchHists = hits.getHits();
    List<Document> results = new ArrayList<>();

    for (SearchHit hit : searchHists) {
      ElasticSearchResult elasticSearchResult = parseSearchResult(hit.getSourceAsString());
      elasticSearchResult.setId(hit.getId());
      elasticSearchResult.setScore(hit.getScore());

      if(hit.getHighlightFields().get(attachmentField) != null) {
        for(Text fragment : hit.getHighlightFields().get(attachmentField).getFragments()) {
          elasticSearchResult.getHighlightFields().add(fragment.toString());
        }
      }

      results.add(createDocument(elasticSearchResult));
    }

    return results;
  }

  @Override
  public List<Document> getAllDocuments() throws IOException {
    return searchData("", false);
  }

  private QueryBuilder createQueryBuilder(String keyword, boolean fuzzy) {
    if (fuzzy) {
      return new MatchQueryBuilder(attachmentField, keyword)
        .fuzziness(Fuzziness.AUTO)
        .prefixLength(0)
        .maxExpansions(50);
    }
    else {
      return QueryBuilders.matchPhraseQuery(attachmentField, keyword);
    }
  }

  private SearchResponse searchByKeyword(String keyword, boolean fuzzy) throws IOException {
    QueryBuilder queryBuilder = null;
    if (StringUtils.isNotBlank(keyword)) {
      queryBuilder = createQueryBuilder(keyword, fuzzy);
    }

    SearchRequest searchRequest = new SearchRequest(index);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(queryBuilder);
    searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
    searchSourceBuilder.sort(new FieldSortBuilder("createTime").order(SortOrder.ASC));

    HighlightBuilder highlightBuilder = new HighlightBuilder();
/*    HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field("attachment.content");
    highlightTitle.highlighterType("unified");
    highlightBuilder.field(highlightTitle);*/
    HighlightBuilder.Field highlightUser = new HighlightBuilder.Field(attachmentField);
    highlightBuilder.field(highlightUser);
    searchSourceBuilder.highlighter(highlightBuilder);

    searchRequest.source(searchSourceBuilder);

    return client.search(searchRequest,RequestOptions.DEFAULT);
  }

/*  SuggestBuilder createSuggestBuilder() {
    SuggestionBuilder termSuggestionBuilder = SuggestBuilders.termSuggestion("attachment.content").text(keyword);
    SuggestBuilder suggestBuilder = new SuggestBuilder();
    suggestBuilder.addSuggestion("suggest_user", termSuggestionBuilder);
    searchSourceBuilder.suggest(suggestBuilder);
  }*/

  private ElasticSearchResult parseSearchResult(String source) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new JavaTimeModule());
    return mapper.readValue(source, ElasticSearchResult.class);
  }

  private Document createDocument(ElasticSearchResult elasticSearchResult) {
    Document document = new Document();
    document.setFileName(elasticSearchResult.getFilename());
    document.setContent(elasticSearchResult.getAttachment().getContent());
    document.setFileType(elasticSearchResult.getAttachment().getContentType());
    document.setSize(FileUtils.byteCountToDisplaySize(elasticSearchResult.getAttachment().getContentLength()));
    document.setId(elasticSearchResult.getId());
    document.setScore(elasticSearchResult.getScore());
    document.setHighlights(elasticSearchResult.getHighlightFields());
    return document;
  }

  @Override
  public void delete(String id) throws IOException{
    DeleteRequest request = new DeleteRequest(index, id);
    client.delete(request, RequestOptions.DEFAULT);
  }

}
