package com.digitald4.biblical.server;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import java.util.concurrent.atomic.AtomicInteger;

@Api(
    name = "books",
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
public class BookService extends EntityServiceImpl<BibleBook, String> {
  private final ScriptureStore scriptureStore;

  @Inject
  BookService(
      BibleBookStore bibleBookStore, LoginResolver loginResolver, ScriptureStore scriptureStore) {
    super(bibleBookStore, loginResolver);
    this.scriptureStore = scriptureStore;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "books")
  public ImmutableList<BibleBook> getBibleBooks(
      @Named("includeUnreleased") @Nullable boolean includeUnreleased) throws ServiceException {
    try {
      return getStore().list(Query.forList()).getItems().stream()
          .filter(book -> includeUnreleased || book.getUnreleased() == null || !book.getUnreleased())
          .sorted(comparing(BibleBook::getNumber))
          .collect(toImmutableList());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "verseCount")
  public AtomicInteger getVerseCount(
      @Named("version") @Nullable String version, @Named("language") @Nullable String language,
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      Query.List query =
          Query.forList().setFilters(Filter.of("book", book), Filter.of("chapter", chapter));
      if (version != null) query.addFilter(Filter.of("version", version));
      if (language != null) query.addFilter(Filter.of("language", language));
      return new AtomicInteger(
          scriptureStore.list(query).getItems().stream()
              .mapToInt(Scripture::getVerse).max().orElse(0));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
