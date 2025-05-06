package com.digitald4.biblical.server;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.InterlinearFetcher;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

@Api(
    name = "interlinears",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class InterlinearService extends EntityServiceImpl<Interlinear, String> {
  private final InterlinearFetcher interlinearFetcher;
  private final InterlinearStore interlinearStore;
  private final BibleBookStore bibleBookStore;

  @Inject
  InterlinearService(InterlinearStore store, LoginResolver loginResolver,
      InterlinearFetcher interlinearFetcher, BibleBookStore bibleBookStore) {
    super(store, loginResolver);
    this.interlinearStore = store;
    this.interlinearFetcher = interlinearFetcher;
    this.bibleBookStore = bibleBookStore;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindexInterlinear")
  public AtomicInteger reindexInterlinear(
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      return new AtomicInteger(
          interlinearStore.create(interlinearFetcher.fetchInterlinear(bibleBookStore.get(book), chapter)).size());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "migrateInterlinear")
  public AtomicInteger migrateInterlinear(
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      return new AtomicInteger(interlinearStore.create(interlinearStore.getInterlinear(book + " " + chapter)).size());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "deleteInterlinear")
  public AtomicInteger deleteInterlinear(@Named("version") String version,
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      return new AtomicInteger(interlinearStore.delete(interlinearStore
          .list(Query.forList(Filter.of("version", version), Filter.of("book", book), Filter.of("chapter", chapter)))
          .getItems().stream().map(Interlinear::getId).collect(toImmutableList())));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "getReferences")
  public QueryResult<Interlinear> getReferences(@Named("strongsId") @Nullable String strongsId,
      @Named("word") @Nullable String word, @Named("hebrewWord") @Nullable String hebrewWord,
      @Named("pageSize") @DefaultValue("50") int pageSize,
      @Named("pageToken") @DefaultValue("1") int pageToken) throws ServiceException {
    try {
      return interlinearStore.getMatchingReferences(
          HebrewConverter.toStrongsId(strongsId), word, hebrewWord, pageSize, pageToken);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }
}
