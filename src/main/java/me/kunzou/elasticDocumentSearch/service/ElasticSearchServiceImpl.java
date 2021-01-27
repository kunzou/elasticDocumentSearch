package me.kunzou.elasticDocumentSearch.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.kunzou.elasticDocumentSearch.dto.Document;
import me.kunzou.elasticDocumentSearch.dto.SearchResult;
import me.kunzou.elasticDocumentSearch.pojo.ElasticSearchResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
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
import java.util.stream.Collectors;

@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

  @Value("${config.elastic.ip}") private String ip;
  @Value("${config.elastic.port}") private int port;
  @Value("${config.index}") private String index;
  @Value("${config.type}") private String type;
  @Value("${config.pipelineId}") private String pipelineId;
  @Value("${config.pipelineField}")  private String pipelineField;
  @Value("${config.contentField}") private String contentField;
  @Value("${config.titleField}") private String titleField;
  @Value("${config.keywordsField}") private String keywordsField;

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
  public Document addDataByMap(MultipartFile file) throws IOException {
    IndexRequest indexRequest = new IndexRequest(index);
    indexRequest.setPipeline(pipelineId);

    Map<String, Object> map = new HashMap<>();
    map.put(pipelineField, Base64.encodeBase64String(file.getBytes()));
    map.put("createTime", new Date());
    map.put("filename", file.getOriginalFilename());

    indexRequest.source(map);
    return getDocument(client.index(indexRequest, RequestOptions.DEFAULT).getId());
  }

  @Override
  public Document getDocument(String id) throws IOException {
    GetRequest getRequest = new GetRequest(index, id);
    GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
    ElasticSearchResult elasticSearchResult = parseSearchResult(getResponse.getSourceAsString());
    return createDocument(elasticSearchResult, getResponse.getId());
  }

  @Override
  public List<SearchResult> searchData(String keyword) throws IOException {
    MultiSearchResponse multiResponse = multiSearch(keyword);

    Map<String, SearchResult> map = new HashMap<>();

    for(MultiSearchResponse.Item item : multiResponse.getResponses()) {
      SearchResponse response = item.getResponse();
      SearchHits hits = response.getHits();
      SearchHit[] searchHists = hits.getHits();

      for (SearchHit hit : searchHists) {
        map.merge(hit.getId(), createSearchResult(hit), this::mergeSearchResult);
      }
    }

    return map.values().stream()
      .sorted(Comparator.comparing(SearchResult::getScore, Comparator.reverseOrder()))
      .collect(Collectors.toList());
  }

  SearchResult createSearchResult(SearchHit hit) throws IOException {
    SearchResult searchResult = new SearchResult();
    ElasticSearchResult elasticSearchResult = parseSearchResult(hit.getSourceAsString());
    searchResult.setId(hit.getId());
    searchResult.setFileName(elasticSearchResult.getFilename());
    searchResult.setScore(hit.getScore());

    if(hit.getHighlightFields().get(contentField) != null) {
      Arrays.stream(hit.getHighlightFields().get(contentField).getFragments()).forEach(fragment->searchResult.getContentHighlights().add(fragment.toString()));
    }
    if(hit.getHighlightFields().get(titleField) != null) {
      Arrays.stream(hit.getHighlightFields().get(titleField).getFragments()).forEach(fragment->searchResult.getTitleHighlights().add(fragment.toString()));
    }
    if(hit.getHighlightFields().get(keywordsField) != null) {
      Arrays.stream(hit.getHighlightFields().get(keywordsField).getFragments()).forEach(fragment->searchResult.getKeywordsHighlights().add(fragment.toString()));
    }

    return searchResult;
  }

  SearchResult mergeSearchResult(SearchResult oldValue, SearchResult newValue) {
    oldValue.setScore(oldValue.getScore()+newValue.getScore());
    oldValue.getKeywordsHighlights().addAll(newValue.getKeywordsHighlights());
    oldValue.getTitleHighlights().addAll(newValue.getTitleHighlights());
    oldValue.getContentHighlights().addAll(newValue.getContentHighlights());
    return oldValue;
  }

  @Override
  public List<Document> getAllDocuments() throws IOException {
    SearchResponse response = searchByKeyword();

    SearchHits hits = response.getHits();
    SearchHit[] searchHists = hits.getHits();
    List<Document> results = new ArrayList<>();

    for (SearchHit hit : searchHists) {
      ElasticSearchResult elasticSearchResult = parseSearchResult(hit.getSourceAsString());
      results.add(createDocument(elasticSearchResult, hit.getId()));
    }

    return results;
  }

  private QueryBuilder createQueryBuilder(String keyword, String field, boolean fuzzy) {
    if(StringUtils.isEmpty(keyword)) {
      return null;
    }

    if (fuzzy) {
      return new MatchQueryBuilder(field, keyword)
        .fuzziness(Fuzziness.AUTO)
        .prefixLength(0)
        .maxExpansions(50);
    }
    else {
      return QueryBuilders.matchPhraseQuery(contentField, keyword);
    }
  }

  private SearchResponse searchByKeyword() throws IOException {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(createQueryBuilder("", contentField, false))
      .size(1000)
      .sort(new ScoreSortBuilder().order(SortOrder.DESC))
      .sort(new FieldSortBuilder("createTime").order(SortOrder.ASC));

    SearchRequest searchRequest = new SearchRequest(index).source(searchSourceBuilder);

    return client.search(searchRequest,RequestOptions.DEFAULT);
  }

  private SearchRequest createSearchRequest(String keyword, String field) {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(createQueryBuilder(keyword, field, true))
      .size(10)
      .sort(new ScoreSortBuilder().order(SortOrder.DESC))
      .sort(new FieldSortBuilder("createTime").order(SortOrder.ASC))
      .highlighter(new HighlightBuilder().field(new HighlightBuilder.Field(field)));

    return new SearchRequest().source(searchSourceBuilder);
  }

  private MultiSearchResponse multiSearch(String keyword) throws IOException {
    MultiSearchRequest request = new MultiSearchRequest()
      .add(createSearchRequest(keyword, contentField))
      .add(createSearchRequest(keyword, titleField))
      .add(createSearchRequest(keyword, keywordsField));
    return client.msearch(request,RequestOptions.DEFAULT);
  }

  private ElasticSearchResult parseSearchResult(String source) throws IOException {
    ElasticSearchResult elasticSearchResult = null;
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new JavaTimeModule());
    elasticSearchResult = mapper.readValue(source, ElasticSearchResult.class);

    return elasticSearchResult;
  }

  private Document createDocument(ElasticSearchResult elasticSearchResult, String id) {
    Document document = new Document();
    document.setFileName(elasticSearchResult.getFilename());
//    document.setContent(elasticSearchResult.getAttachment().getContent());
    document.setFileType(elasticSearchResult.getAttachment().getContentType());
    document.setSize(FileUtils.byteCountToDisplaySize(elasticSearchResult.getAttachment().getContentLength()));
    document.setId(id);

    document.setTitle(elasticSearchResult.getAttachment().getTitle());
    document.setAuthor(elasticSearchResult.getAttachment().getAuthor());

    return document;
  }

  @Override
  public void delete(String id) throws IOException{
    DeleteRequest request = new DeleteRequest(index, id);
    client.delete(request, RequestOptions.DEFAULT);
  }

}
