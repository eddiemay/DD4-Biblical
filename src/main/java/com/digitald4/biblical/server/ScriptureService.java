package com.digitald4.biblical.server;

import static com.digitald4.biblical.model.BibleBook.EN;

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
      @Named("locale") @DefaultValue(EN) String locale) throws ServiceException {
    try {
      return scriptureStore.getScriptures(version, locale, reference);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "search")
  public GetOrSearchResponse search(
      @Named("searchText") String searchText, @Named("version") @Nullable String version,
      @Named("locale") @DefaultValue(EN) String locale,
      @Named("orderBy") @DefaultValue(ScriptureStore.DEFAULT_ORDER_BY) String orderBy,
      @Named("pageSize") @DefaultValue("50") int pageSize,
      @Named("pageToken") @DefaultValue("1") int pageToken)
      throws ServiceException {
    try {
      return scriptureReferenceProcessor.matchesPattern(searchText)
          ? scriptureStore.getScriptures(version, locale, searchText)
          : GetOrSearchResponse.searchResult(
              scriptureStore.search(Query.forSearch(searchText, orderBy, pageSize, pageToken)));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindex")
  public Empty reindex(@Named("version") String version,
      @Named("locale") @DefaultValue(EN) String locale,
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      scriptureStore.reindex(version, locale, book, chapter);
      return Empty.getInstance();
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
}
