package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;

public class ScriptureFetcherRouter implements ScriptureFetcher {
  private final ScriptureFetcherBibleGateway bibleGateway;
  private final ScriptureFetcherBibleHub bibleHub;
  private final ScriptureFetcherJWOrg jwOrg;
  private final ScriptureFetcherKJV1611 kjv1611;
  private final ScriptureFetcherOneOff oneOff;
  private final ScriptureFetcherPseudepigrapha pseudepigrapha;
  private final ScriptureFetcherStepBibleOrg stepBibleOrg;

  @Inject
  public ScriptureFetcherRouter(
      ScriptureFetcherBibleGateway bibleGateway,
      ScriptureFetcherBibleHub bibleHub,
      ScriptureFetcherJWOrg jwOrg,
      ScriptureFetcherKJV1611 kjv1611,
      ScriptureFetcherOneOff oneOff,
      ScriptureFetcherPseudepigrapha pseudepigrapha,
      ScriptureFetcherStepBibleOrg stepBibleOrg) {
    this.bibleGateway = bibleGateway;
    this.bibleHub = bibleHub;
    this.oneOff = oneOff;
    this.jwOrg = jwOrg;
    this.kjv1611 = kjv1611;
    this.pseudepigrapha = pseudepigrapha;
    this.stepBibleOrg = stepBibleOrg;
  }

  @Override
  public ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    return getFetcher(version).fetch(version, book, chapter);
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
      case "essene":
      case "qumran":
      case "uchicago":
      case "mrjames":
        return oneOff;
      case "OXFORD":
        return pseudepigrapha;
      case "RSKJ":
        return stepBibleOrg;
      default:
        return bibleHub;
    }
  }
}
