package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;

public class ScriptureFetcherRouter implements ScriptureFetcher {
  private final ScriptureFetcherBibleGateway bibleGateway;
  private final ScriptureFetcherBibleHub bibleHub;
  private final ScriptureFetcherJWOrg jwOrg;
  private final ScriptureFetcherKJV1611 kjv1611;
  private final ScriptureFetcherOneOff oneOff;
  private final ScriptureFetcherPseudepigrapha pseudepigrapha;
  private final ScriptureFetcherSefariaOrg sefariaOrg;
  private final ScriptureFetcherStepBibleOrg stepBibleOrg;

  @Inject
  public ScriptureFetcherRouter(APIConnector apiConnector) {
    this.bibleGateway = new ScriptureFetcherBibleGateway(apiConnector);
    this.bibleHub = new ScriptureFetcherBibleHub(apiConnector);
    this.oneOff = new ScriptureFetcherOneOff(apiConnector);
    this.jwOrg = new ScriptureFetcherJWOrg(apiConnector);
    this.kjv1611 = new ScriptureFetcherKJV1611(apiConnector);
    this.pseudepigrapha = new ScriptureFetcherPseudepigrapha(apiConnector);
    this.sefariaOrg = new ScriptureFetcherSefariaOrg(apiConnector);
    this.stepBibleOrg = new ScriptureFetcherStepBibleOrg(apiConnector);
  }

  @Override
  public ImmutableList<Scripture> fetch(String version, String language, BibleBook book, int chapter) {
    return getFetcher(version).fetch(version, language, book, chapter);
  }

  private ScriptureFetcher getFetcher(String version) {
    return switch (version) {
      case "NKJV", "NRSV", "RSV", "WYC" -> bibleGateway;
      case "NWT" -> jwOrg;
      case "KJV1611" -> kjv1611;
      case "DSS", "CCC", "SID" -> oneOff;
      case "OXFORD" -> pseudepigrapha;
      case "Sefaria" -> sefariaOrg;
      case "RSKJ" -> stepBibleOrg;
      default -> bibleHub;
    };
  }
}
