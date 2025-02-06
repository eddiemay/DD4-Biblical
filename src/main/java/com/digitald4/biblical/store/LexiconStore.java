package com.digitald4.biblical.store;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import javax.inject.Inject;
import javax.inject.Provider;

public class LexiconStore extends GenericStore<Lexicon, String> {
  private final LexiconFetcher lexiconFetcher;
  @Inject
  public LexiconStore(Provider<DAO> dao, LexiconFetcher lexiconFetcher) {
    super(Lexicon.class, dao);
    this.lexiconFetcher = lexiconFetcher;
  }

  @Override
  public Lexicon get(String strongsId) {
    if (strongsId == null) {
      return null;
    }
    Lexicon lexicon = super.get(HebrewConverter.toStrongsId(strongsId));
    if (lexicon == null || lexicon.translation() == null) {
      lexicon = create(lexiconFetcher.getLexicon(strongsId));
    }
    return lexicon;
  }
}
