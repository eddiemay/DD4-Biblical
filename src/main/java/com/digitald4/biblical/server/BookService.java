package com.digitald4.biblical.server;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.common.exception.DD4StorageException;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableSet;
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
public class BookService {
  private final ScriptureStore scriptureStore;

  @Inject
  BookService(ScriptureStore scriptureStore) {
    this.scriptureStore = scriptureStore;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "books")
  public ImmutableSet<BibleBook> getBibleBooks() throws ServiceException {
    try {
      return BibleBook.ALL_BOOKS;
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "verseCount")
  public AtomicInteger getVerseCount(@Named("book") String book, @Named("chapter") int chapter,
                                     @Named("version") @Nullable String version) throws ServiceException {
    try {
      return new AtomicInteger(
          scriptureStore.getScriptures(version, book + " " + chapter).getItems().stream()
              .mapToInt(Scripture::getVerse).max().orElse(0));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
