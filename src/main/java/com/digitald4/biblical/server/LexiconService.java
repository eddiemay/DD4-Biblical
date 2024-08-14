package com.digitald4.biblical.server;

import static com.digitald4.biblical.util.HebrewConverter.toStrongsId;
import static com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl.processScriptureReferences;
import static com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl.processStrongsReferences;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.IntStream.range;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.LoginResolver;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

@Api(
    name = "lexicons",
    version = "v1",
    namespace = @ApiNamespace(ownerDomain = "biblical.digitald4.com", ownerName = "biblical.digitald4.com")
)
public class LexiconService extends EntityServiceImpl<Lexicon, String> {
  private final LexiconFetcher lexiconFetcher;
  private final InterlinearStore interlinearStore;
  private final TokenWordStore tokenWordStore;

  @Inject
  LexiconService(LexiconStore store, LoginResolver loginResolver, LexiconFetcher lexiconFetcher,
      InterlinearStore interlinearStore, TokenWordStore tokenWordStore) {
    super(store, loginResolver);
    this.lexiconFetcher = lexiconFetcher;
    this.interlinearStore = interlinearStore;
    this.tokenWordStore = tokenWordStore;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "reindex")
  public AtomicInteger reindex(@Named("startIndex") int startIndex, @Named("endIndex") int endIndex,
      @Named("language") @DefaultValue("H") String lang) throws ServiceException {
    try {
        return new AtomicInteger(getStore().create(range(startIndex, endIndex)
            .mapToObj(id -> toStrongsId(lang + id)).map(lexiconFetcher::getLexicon).collect(toImmutableList())).size());
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
                  .get(range(startIndex, endIndex).mapToObj(id -> toStrongsId(lang + id)).collect(toImmutableList()))
                  .getItems().stream()
                  .map(LexiconService::processReferences)
                  .collect(toImmutableList())).size());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "fillReferenceCount")
  public AtomicInteger fillReferenceCount(@Named("strongsId") String strongsId) throws ServiceException {
    try {
      String id = HebrewConverter.toStrongsId(strongsId);
      Lexicon lexicon = getStore().get(id);
      if (lexicon.getReferenceCount() == null) {
        lexicon = getStore().update(
            id, l -> l.setReferenceCount(interlinearStore.getMatchingReferences(id, null, null, 5, 1).getTotalSize()));
        tokenWordStore.reset();
      }
      return new AtomicInteger(lexicon.getReferenceCount());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  protected boolean requiresLogin(String method) {
    return !method.equals("get") && !method.equals("list") && !method.equals("search") && super.requiresLogin(method);
  }

  private static Lexicon processReferences(Lexicon lexicon) {
    return lexicon
        .setRootWord(processStrongsReferences(lexicon.getRootWord()))
        .setStrongsDefinition(
            processScriptureReferences(processStrongsReferences(lexicon.getStrongsDefinition().toString())));
  }
}
