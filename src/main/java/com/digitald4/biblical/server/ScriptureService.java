package com.digitald4.biblical.server;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.ScriptureStore.GetOrSearchResponse;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.Empty;
import com.digitald4.common.server.service.EntityServiceBulkImpl;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Query;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import java.util.concurrent.atomic.AtomicInteger;

@Api(
    name = "scriptures",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class ScriptureService extends EntityServiceBulkImpl<String, Scripture> {
  private final ScriptureStore scriptureStore;
  private final ScriptureReferenceProcessor scriptureReferenceProcessor;

  @Inject
  ScriptureService(ScriptureStore scriptureStore, LoginResolver loginResolver,
                   ScriptureReferenceProcessor scriptureReferenceProcessor) {
    super(scriptureStore, loginResolver);
    this.scriptureStore = scriptureStore;
    this.scriptureReferenceProcessor = scriptureReferenceProcessor;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "scriptures")
  public GetOrSearchResponse getScriptures(
      @Named("reference") String reference, @Named("version") @Nullable String version,
      @Named("lang") @Nullable String lang) throws ServiceException {
    try {
      return scriptureStore.getScriptures(version, lang, reference);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "fetch")
  public GetOrSearchResponse fetch(@Named("searchText") String searchText,
      @Named("version") @Nullable String version, @Named("lang") @Nullable String lang,
      @Named("pageSize") @DefaultValue("50") int pageSize, @Named("pageToken") @DefaultValue("1") int pageToken,
      @Named("orderBy") @DefaultValue(ScriptureStore.DEFAULT_ORDER_BY) String orderBy) throws ServiceException {
    try {
      return scriptureReferenceProcessor.matchesPattern(searchText)
          ? scriptureStore.getScriptures(version, lang, searchText)
          : GetOrSearchResponse.searchResult(
              scriptureStore.search(Query.forSearch(searchText, orderBy, pageSize, pageToken)));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindex")
  public Empty reindex(@Named("version") String version, @Named("lang") @Nullable String lang,
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      scriptureStore.reindex(version, lang, book, chapter);
      return Empty.getInstance();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "migrateScriptures")
  public AtomicInteger migrateScriptures(@Named("version") String version, @Named("lang") @Nullable String lang,
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      return new AtomicInteger(scriptureStore.migrate(version, lang, book, chapter));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "searchAndReplace")
  public ImmutableList<Scripture> searchAndReplace(
      @Named("phrase") String phrase, @Named("replacement") String replacement,
      @Named("filter") String filter, @Named("preview") @Nullable boolean preview,
      @Named("idToken") @Nullable String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken, true);
      return scriptureStore.searchAndReplace(phrase, replacement, filter, preview);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "searchAndDelete")
  public AtomicInteger searchAndDelete(@Named("searchText") String searchText,
      @Named("idToken") @Nullable String idToken) throws ServiceException {
    try {
      resolveLogin(idToken, true);
      return new AtomicInteger(scriptureStore.searchAndDelete(searchText));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "uploadScripture")
  public ImmutableList<Scripture> uploadScripture(
      @Named("version") String version, @Named("lang") @DefaultValue("en") String lang,
      @Named("book") String book, @Named("chapter") int chapter, @Named("text") String text,
      @Named("preview") @Nullable boolean preview,
      @Named("idToken") @Nullable String idToken) throws ServiceException {
    try {
      resolveLogin(idToken, true);
      return scriptureStore.uploadScripture(version, lang, book, chapter, text, preview);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
