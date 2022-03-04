package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;

public class ScriptureFetcherRouter implements ScriptureFetcher {
  private final ScriptureFetcherBibleGateway bibleGateway;
  private final ScriptureFetcherBibleHub bibleHub;
  private final ScriptureFetcherBookOfEnochReferences bookOfEnochReferences;
  private final ScriptureFetcherJWOrg jwOrg;
  private final ScriptureFetcherKJV1611 kjv1611;
  private final ScriptureFetcherPseudepigrapha pseudepigrapha;
  private final ScriptureFetcherStepBibleOrg stepBibleOrg;

  @Inject
  public ScriptureFetcherRouter(
      ScriptureFetcherBibleGateway bibleGateway,
      ScriptureFetcherBibleHub bibleHub,
      ScriptureFetcherBookOfEnochReferences bookOfEnochReferences,
      ScriptureFetcherJWOrg jwOrg,
      ScriptureFetcherKJV1611 kjv1611,
      ScriptureFetcherPseudepigrapha pseudepigrapha,
      ScriptureFetcherStepBibleOrg stepBibleOrg) {
    this.bibleGateway = bibleGateway;
    this.bibleHub = bibleHub;
    this.bookOfEnochReferences = bookOfEnochReferences;
    this.jwOrg = jwOrg;
    this.kjv1611 = kjv1611;
    this.pseudepigrapha = pseudepigrapha;
    this.stepBibleOrg = stepBibleOrg;
  }

  @Override
  public ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    return getFetcher(version).fetch(version, book, chapter);
  }

  @Override
  public String getChapterUrl(String version, ScriptureReferenceProcessor.VerseRange verseRange) {
    return getFetcher(version).getChapterUrl(version, verseRange);
  }

  @Override
  public String getVerseUrl(Scripture scripture) {
    return getFetcher(scripture.getVersion()).getVerseUrl(scripture);
  }

  private ScriptureFetcher getFetcher(String version) {
    switch (version) {
      case "NKJV":
      case "NRSV":
      case "RSV":
      case "WYC":
        return bibleGateway;
      case "NWT":
        return jwOrg;
      case "KJV1611":
        return kjv1611;
      case "EnochRef":
        return bookOfEnochReferences;
      case "OXFORD":
        return pseudepigrapha;
      case "RSKJ":
        return stepBibleOrg;
      default:
        return bibleHub;
    }
  }
}
