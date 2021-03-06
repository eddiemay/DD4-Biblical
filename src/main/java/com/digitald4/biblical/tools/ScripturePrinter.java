package com.digitald4.biblical.tools;

import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.*;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.testing.DAOTestingImpl;

public class ScripturePrinter {
  public static void main(String[] args) {
    String version = "ISR";
    DAOTestingImpl dao = new DAOTestingImpl();
    APIConnector apiConnector = new APIConnector(null, null, 100);
    ScriptureStore scriptureStore = new ScriptureStore(
        () -> dao, null,
        new ScriptureReferenceProcessorSplitImpl(),
        new ScriptureFetcherRouter(
            new ScriptureFetcherBibleGateway(apiConnector),
            new ScriptureFetcherBibleHub(apiConnector),
            new ScriptureFetcherBookOfEnochReferences(apiConnector),
            new ScriptureFetcherJWOrg(apiConnector),
            new ScriptureFetcherKJV1611(apiConnector),
            new ScriptureFetcherPseudepigrapha(apiConnector),
            new ScriptureFetcherStepBibleOrg(apiConnector)));

    if (args.length > 1 && !args[1].isEmpty()) {
      version = args[1];
    }

    scriptureStore.getScriptures(version, args[0]).forEach(System.out::println);
  }
}
