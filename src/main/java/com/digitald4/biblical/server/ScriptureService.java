package com.digitald4.biblical.server;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.server.service.Empty;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SessionStore;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@Api(
    name = "scriptures",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    ),
    // [START_EXCLUDE]
    issuers = {
        @ApiIssuer(
            name = "firebase",
            issuer = "https://securetoken.google.com/biblical",
            jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
    }
    // [END_EXCLUDE]
)
public class ScriptureService extends EntityServiceImpl<Scripture> {
  public static final String DEFAULT_VERSION = "ISR";

  private final ScriptureStore scriptureStore;
  private final ScriptureReferenceProcessor scriptureReferenceProcessor;

  @Inject
  ScriptureService(ScriptureStore scriptureStore, SessionStore<BasicUser> sessionStore,
                   ScriptureReferenceProcessor scriptureReferenceProcessor) {
    super(scriptureStore, sessionStore, true);
    this.scriptureStore = scriptureStore;
    this.scriptureReferenceProcessor = scriptureReferenceProcessor;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "scriptures")
  public ImmutableList<Scripture> getScriptures(
      @Named("reference") String reference, @Named("version") @Nullable String version) throws ServiceException {
    try {
      return scriptureStore.getScriptures(version, reference);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "search")
  public QueryResult<Scripture> search(
      @Named("searchText") String searchText, @Named("version") @Nullable String version,
      @Named("orderBy") @DefaultValue(ScriptureStore.DEFAULT_ORDER_BY) String orderBy,
      @Named("pageSize") @DefaultValue("200") int pageSize, @Named("pageToken") @DefaultValue("1") int pageToken)
      throws ServiceException {
    try {
      return scriptureReferenceProcessor.matchesPattern(searchText)
          ? GetOrSearchResponse.getResult(scriptureStore.getScriptures(version, searchText))
          : GetOrSearchResponse.searchResult(
              scriptureStore.search(Query.forSearch(searchText, orderBy, pageSize, pageToken)));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "expand")
  public StringBuilder expand(
      @Named("html") String html, @Named("version") @DefaultValue(DEFAULT_VERSION) String version,
      @DefaultValue("false") @Named("includeLinks") boolean includeLinks) throws ServiceException {
    try {
      return new StringBuilder(scriptureStore.expandScriptures(version, html, includeLinks));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindex")
  public Empty reindex(@Named("version") String version, @Named("book") String book, @Named("chapter") int chapter)
      throws ServiceException {
    try {
      scriptureStore.reindex(version, book, chapter);
      return Empty.getInstance();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "searchAndReplace")
  public ImmutableList<Scripture> searchAndReplace(
      @Named("phrase") String phrase, @Named("replacement") String replacement, @Named("filter") String filter,
      @Named("preview") @Nullable boolean preview, @Named("idToken") @Nullable String idToken) throws ServiceException {
    try {
      resolveLogin(idToken, true);
      return scriptureStore.searchAndReplace(phrase, replacement, filter, preview);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "searchAndDelete")
  public Empty searchAndDelete(@Named("searchText") String searchText, @Named("idToken") @Nullable String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken, true);
      scriptureStore.searchAndDelete(searchText);
      return Empty.getInstance();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  public static class GetOrSearchResponse extends QueryResult<Scripture> {
    private enum RESULT_TYPE {GET, SEARCH};
    private final RESULT_TYPE resultType;

    private GetOrSearchResponse(RESULT_TYPE resultType, Iterable<Scripture> scriptures, int totalSize, Query query) {
      super(scriptures, totalSize, query);
      this.resultType = resultType;
    }

    public RESULT_TYPE getResultType() {
      return resultType;
    }

    private static GetOrSearchResponse getResult(ImmutableList<Scripture> scriptures) {
      return new GetOrSearchResponse(RESULT_TYPE.GET, scriptures, scriptures.size(), null);
    }

    private static GetOrSearchResponse searchResult(QueryResult<Scripture> queryResult) {
      return new GetOrSearchResponse(
          RESULT_TYPE.SEARCH, queryResult.getItems(), queryResult.getTotalSize(), queryResult.query());
    }
  }
}
