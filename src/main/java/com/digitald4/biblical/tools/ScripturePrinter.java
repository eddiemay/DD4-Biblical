package com.digitald4.biblical.tools;

import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.*;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.testing.DAOTestingImpl;

public class ScripturePrinter {
  public static void main(String[] args) {
    String version = "ISR";
    DAOTestingImpl dao = new DAOTestingImpl(new ChangeTracker(null, null, new SearchIndexer() {
      @Override
      public <T extends Searchable> void index(Iterable<T> iterable) {}

      @Override
      public <T extends Searchable> QueryResult<T> search(Class<T> aClass, Search search) {
        return null;
      }

      @Override
      public <T extends Searchable> void removeIndex(Class<T> aClass, Iterable<?> iterable) {}
    }, null));
    APIConnector apiConnector = new APIConnector(null, null, 100);
    ScriptureStore scriptureStore = new ScriptureStore(
        () -> dao, null,
        new ScriptureReferenceProcessorSplitImpl(),
        new ScriptureFetcherRouter(
            new ScriptureFetcherBibleGateway(apiConnector),
            new ScriptureFetcherBibleHub(apiConnector),
            new ScriptureFetcherJWOrg(apiConnector),
            new ScriptureFetcherKJV1611(apiConnector),
            new ScriptureFetcherOneOff(apiConnector),
            new ScriptureFetcherPseudepigrapha(apiConnector),
            new ScriptureFetcherStepBibleOrg(apiConnector)));

    if (args.length > 1 && !args[1].isEmpty()) {
      version = args[1];
    }

    scriptureStore.getScriptures(version, "en", args[0]).getItems().forEach(System.out::println);
  }
}
