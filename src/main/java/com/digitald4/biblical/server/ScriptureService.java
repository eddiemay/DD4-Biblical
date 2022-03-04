package com.digitald4.biblical.server;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.ScriptureStore;
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
import com.google.common.collect.ImmutableSet;
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

  @Inject
  ScriptureService(ScriptureStore scriptureStore, SessionStore<BasicUser> sessionStore) {
    super(scriptureStore, sessionStore, true);
    this.scriptureStore = scriptureStore;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "scriptures")
  public ImmutableList<Scripture> getScriptures(
      @Nullable @Named("version") String version, @Named("reference") String reference) throws ServiceException {
    try {
      return scriptureStore.getScriptures(version != null ? version: DEFAULT_VERSION, reference);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "search")
  public QueryResult<Scripture> search(
      @Named("searchText") String searchText,
      @Named("orderBy") @DefaultValue("bookNum,chapter,verse,versionNum,version") String orderBy,
      @Named("pageSize") @DefaultValue("200") int pageSize, @Named("pageToken") @DefaultValue("1") int pageToken)
      throws ServiceException {
    try {
      return scriptureStore.search(Query.forSearch(searchText, orderBy, pageSize, pageToken));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "searchAndDelete")
  public Empty searchAndDelete(@Named("searchText") String searchText) throws ServiceException {
    try {
      scriptureStore.searchAndDelete(searchText);
      return Empty.getInstance();
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

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "html")
  public StringBuilder getScripturesHtml(
      @Nullable @Named("version") String version,
      @Named("reference") String reference,
      @DefaultValue("false") @Named("includeLinks") boolean includeLinks,
      @DefaultValue("false") @Named("spaceVerses") boolean spaceVerses) throws ServiceException {
    try {
      return new StringBuilder(
          scriptureStore.getScripturesHtml(
              version != null ? version: DEFAULT_VERSION, reference, includeLinks, spaceVerses));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "expand")
  public StringBuilder expand(
      @Nullable @Named("version") String version, @Named("html") String html,
      @DefaultValue("false") @Named("includeLinks") boolean includeLinks) throws ServiceException {
    try {
      return new StringBuilder(
          scriptureStore.expandScriptures(version != null ? version: DEFAULT_VERSION, html, includeLinks));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "books")
  public ImmutableSet<BibleBook> getBibleBooks() throws ServiceException {
    try {
      return BibleBook.ALL_BOOKS;
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
