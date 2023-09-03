package com.digitald4.biblical.tools;

import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.util.*;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOApiImpl;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.testing.DAOTestingImpl;

public class ScripturePrinter {
  private final static String API_URL = "https://dd4-biblical.appspot.com/_api";
  private final static String API_VERSION = "v1";
  public static void main(String[] args) {
    String version = "ISR";
    String language = "en";
    String reference = null;
    String idToken = null;
    boolean useApi = false;
    for (int a = 0; a < args.length; a++) {
      if (args[a].isEmpty()) {
        break;
      }
      switch (args[a]) {
        case "--version": version = args[++a]; break;
        case "--language": language = args[++a]; break;
        case "--useApi": useApi = true; break;
        case "--idToken": idToken = args[++a]; break;
        default: reference = args[a];
      }
    }
    APIConnector apiConnector = new APIConnector(API_URL, API_VERSION, 100).setIdToken(idToken);
    DAO dao = useApi ? new DAOApiImpl(apiConnector)
        : new DAOTestingImpl(new ChangeTracker(null, null, new SearchIndexer() {
            @Override
            public <T extends Searchable> void index(Iterable<T> iterable) {}

            @Override
            public <T extends Searchable> QueryResult<T> search(Class<T> aClass, Search search) {
              return null;
            }

            @Override
            public <T extends Searchable> void removeIndex(Class<T> aClass, Iterable<?> iterable) {}
          }, null));
    StaticDataDAO staticDataDAO = new StaticDataDAO();
    BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
    ScriptureStore scriptureStore = new ScriptureStore(
        () -> dao, null, bibleBookStore,
        new ScriptureReferenceProcessorSplitImpl(bibleBookStore),
        new ScriptureFetcherRouter(
            new ScriptureFetcherBibleGateway(apiConnector),
            new ScriptureFetcherBibleHub(apiConnector),
            new ScriptureFetcherJWOrg(apiConnector),
            new ScriptureFetcherKJV1611(apiConnector),
            new ScriptureFetcherOneOff(apiConnector),
            new ScriptureFetcherPseudepigrapha(apiConnector),
            new ScriptureFetcherSefariaOrg(apiConnector),
            new ScriptureFetcherStepBibleOrg(apiConnector)));

    scriptureStore.getScriptures(version, language, reference).getItems().forEach(System.out::println);
  }
}
