package com.digitald4.biblical.store;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import javax.inject.Inject;
import javax.inject.Provider;

public class LexiconStore extends GenericStore<Lexicon, String> {
  @Inject
  public LexiconStore(Provider<DAO> dao, LexiconFetcher lexiconFetcher) {
    super(Lexicon.class, dao);
  }

  @Override
  public Lexicon get(String strongsId) {
    if (strongsId == null) {
      return null;
    }
    return super.get(HebrewConverter.toStrongsId(strongsId));
  }
}
