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
    )
)
public class BookService extends EntityServiceImpl<BibleBook, String> {
  private final ScriptureStore scriptureStore;

  @Inject
  BookService(BibleBookStore bibleBookStore, LoginResolver loginResolver, ScriptureStore scriptureStore) {
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
      Query.List query = Query.forList().setFilters(Filter.of("book", book), Filter.of("chapter", chapter));
      if (version != null) query.addFilter(Filter.of("version", version));
      if (language != null) query.addFilter(Filter.of("language", language));
      return new AtomicInteger(
          scriptureStore.list(query).getItems().stream().mapToInt(Scripture::getVerse).max().orElse(0));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  protected boolean requiresLogin(String method) {
    return !"get".equals(method) && super.requiresLogin(method);
  }
}

/*
At Res 10:
  Original 33.73574074074074%
  Grayscale 31.01203703703704%
  Med Blur 44.99333333333333%
  Gau Blur 35.87870370370371%
  Med Thres 120: 49.04444444444444%
  Gau Thres 120: 46.5396296296296%
  Med Thres 140: 54.375370370370376%
  Gau Thres 140: 50.71555555555554%

At Res 8:
  Original 42.13574074074075%
  Grayscale 41.9687037037037%
  Med Blur 19.198148148148146%
  Gau Blur 35.46425925925926%
  Med Thres 120: 20.06537037037037%
  Gau Thres 120: 45.711481481481485%
  Med Thres 140: 28.72592592592593%
  Gau Thres 140: 55.96111111111113%

At Res 9:
  Original 35.48592592592592%
  Grayscale 37.33777777777778%
  Med Blur 46.10944444444445%
  Gau Blur 40.23111111111111%
  Med Thres 120: 47.29%
  Gau Thres 120: 53.40%
  Med Thres 130: 52.34%
  Gau Thres 130: 58.25%
  Med Thres 132: 53.25%
  Gau Thres 132: 59.45%
  Med Thres 133: 54.87%
  Gau Thres 133: 59.21%
  Med Thres 135: 54.63%
  Gau Thres 135: 59.10%
  Med Thres 136: 54.65%
  Gau Thres 136: 58.32%
  Med Thres 137: 54.53%
  Gau Thres 137: 58.02%
  Med Thres 138: 54.79%
  Gau Thres 138: 57.03%
  Med Thres 140: 55.33%
  Gau Thres 140: 57.85%
  Med Thres 142: 54.56%
  Gau Thres 142: 56.27%
  Med Thres 144: 55.34%
  Gau Thres 144: 55.64%
  Med Thres 145: 55.42%
  Gau Thres 145: 53.61%
  Med Thres 146: 54.59%
  Gau Thres 146: 52.68%
  Med Thres 147: 54.59%
  Gau Thres 147: 52.78%
  Med Thres 150: 52.79%
  Gau Thres 150: 49.70%
  Med Thres 160: 43.19%
  Gau Thres 160: 39.576851851851856%

  [35.49 37.34 46.11 40.23 55.42 59.45], Gau Thres
  [35.72 36.14 49.36 41.32 56.62 61.4 ], Gau Thres

  [35.49 37.34 46.11 40.23 55.7  59.48], Gau Thres
  [35.72 36.14 49.36 41.32 57.18 61.47], Gau Thres

  [35.49 37.34 46.11 41.04 55.42 58.16], Gau Thres
  [35.72 36.14 49.36 43.42 56.62 61.94], Gau Thres

  [35.49 37.34 46.11 41.52 55.42 58.71], Gau Thres
  [35.72 36.14 49.36 42.93 56.62 63.05], Gau Thres


  [35.49 37.34 45.   41.52 55.47 58.71], Gau Thres
  [35.72 36.14 50.59 42.93 57.85 63.05], Gau Thres
  [35.49 37.34 45.   41.52 57.68 61.05 41.14], Gau Thres
  [35.72 36.14 50.59 42.93 60.56 65.78 42.72], Gau Thres

  Playing with 2x and using psm=6
  [41.43 42.78 43.27 40.92 51.42 54.71 36.87 40.04], Gau Thres
  [41.33 42.86 45.66 39.17 49.08 53.34 36.34 37.24], Gau Thres

  [31.75 33.29 39.8  36.67 51.41 54.42 32.08 36.4 ], Gau Thres
  [31.58 33.3  45.9  38.18 54.63 60.06 31.27 38.12], Gau Thres

  [35.99 37.87 45.59 42.15 58.14 61.42 37.43 41.47], Gau Thres
  [36.38 36.75 50.38 44.03 61.02 65.94 36.28 43.39], Gau Thres

 */
