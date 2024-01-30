package com.digitald4.biblical.server;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.util.InterlinearFetcher;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.inject.Inject;

@Api(
    name = "lexicons",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class LexiconService extends EntityServiceImpl<Lexicon, String> {
  private final LexiconFetcher lexiconFetcher;
  private final InterlinearFetcher interlinearFetcher;
  private final InterlinearStore interlinearStore;
  private final BibleBookStore bibleBookStore;

  @Inject
  LexiconService(LexiconStore store, LoginResolver loginResolver,
      LexiconFetcher lexiconFetcher, InterlinearFetcher interlinearFetcher,
      InterlinearStore interlinearStore, BibleBookStore bibleBookStore) {
    super(store, loginResolver);
    this.lexiconFetcher = lexiconFetcher;
    this.interlinearStore = interlinearStore;
    this.bibleBookStore = bibleBookStore;
    this.interlinearFetcher = interlinearFetcher;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindex")
  public AtomicInteger reindex(@Named("startIndex") int startIndex, @Named("endIndex") int endIndex,
      @Named("language") @DefaultValue("H") String language) throws ServiceException {
    try {
        return new AtomicInteger(
            getStore().create(
              IntStream.range(startIndex, endIndex)
                  .mapToObj(id -> language + id)
                  .map(lexiconFetcher::getLexicon)
                  .collect(toImmutableList())).size());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "migrateLexicon")
  public AtomicInteger migrateLexicon(
      @Named("startIndex") int startIndex, @Named("endIndex") int endIndex,
      @Named("lang") @DefaultValue("H") String lang) throws ServiceException {
    try {
      return new AtomicInteger(
          getStore().create(
              getStore()
                  .get(
                      IntStream.range(startIndex, endIndex)
                          .mapToObj(id -> lang + id)
                          .collect(toImmutableList()))
                  .stream()
                  .peek(LexiconService::processReferences)
                  .collect(toImmutableList())).size());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindexInterlinear")
  public AtomicInteger reindexInterlinear(
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      return new AtomicInteger(
          interlinearStore.create(
              interlinearFetcher.fetchInterlinear(bibleBookStore.get(book), chapter)).size());
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
      return new AtomicInteger(
          interlinearStore.create(interlinearStore.getInterlinear(book + " " + chapter)).size());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "deleteInterlinear")
  public AtomicInteger deleteInterlinear(
      @Named("book") String book, @Named("chapter") int chapter) throws ServiceException {
    try {
      return new AtomicInteger(
          interlinearStore.delete(
              interlinearStore
                  .list(Query.forList(Filter.of("book", book), Filter.of("chapter", chapter)))
                  .getItems().stream().map(Interlinear::getId).collect(toImmutableList())));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "getReferences")
  public QueryResult<Interlinear> getReferences(
      @Named("interlinearId") @Nullable String interlinearId,
      @Named("matchCriteria") @DefaultValue("0") int matchCriteria,
      @Named("strongsId") @Nullable String strongsId,
      @Named("word") @Nullable String word,
      @Named("hebrewWord") @Nullable String hebrewWord,
      @Named("pageSize") @DefaultValue("50") int pageSize,
      @Named("pageToken") @DefaultValue("1") int pageToken) throws ServiceException {
    try {
      if (interlinearId != null) {
        Interlinear interlinear = interlinearStore.get(interlinearId);
        if ((matchCriteria & 4) > 0 || matchCriteria == 0) strongsId = interlinear.getStrongsId();
        if ((matchCriteria & 2) > 0 || matchCriteria == 0) word = interlinear.getWord();
        if ((matchCriteria & 1) > 0 || matchCriteria == 0) hebrewWord = interlinear.getConstantsOnly();
      }
      return
          interlinearStore.getMatchingReferences(strongsId, word, hebrewWord, pageSize, pageToken);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @Override
  protected boolean requiresLogin(String method) {
    return !method.equals("get") && !method.equals("list") && super.requiresLogin(method);
  }

  private static Lexicon processReferences(Lexicon lexicon) {
    return lexicon
        .setRootWord(LexiconFetcherBlueLetterImpl.processStrongsReferences(lexicon.getRootWord()))
        .setStrongsDefinition(
            LexiconFetcherBlueLetterImpl.processScriptureReferences(
                LexiconFetcherBlueLetterImpl.processStrongsReferences(
                    lexicon.getStrongsDefinition().toString())))
        /* .setBrownDriverBriggs(
            LexiconFetcherBlueLetterImpl.processScriptureReferences(
                LexiconFetcherBlueLetterImpl.processStrongsReferences(
                    lexicon.getBrownDriverBriggs().toString()))) */ ;
  }
}
