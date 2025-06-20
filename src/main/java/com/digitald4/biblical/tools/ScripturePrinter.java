package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.IntStream.range;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.*;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.storage.Transaction.Op;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.digitald4.common.util.JSONUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScripturePrinter {
  private final static String BOOK_URL = "%s/books?includeUnreleased=true";
  public static void main(String[] args) {
    String version = "ISR";
    String language = "gez";
    String reference = "Isa 1:1";
    boolean useApi = false;
    for (int a = 0; a < args.length; a++) {
      if (args[a].isEmpty()) {
        break;
      }
      switch (args[a]) {
        case "--version" -> version = args[++a];
        case "--language", "--lang" -> language = args[++a];
        case "--useApi" -> useApi = true;
        default -> reference = args[a];
      }
    }
    APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 100);
    DAO dao = useApi ? new DAOApiImpl(apiConnector)
        : new DAOTestingImpl(new ChangeTracker( null, null, new SearchIndexer() {
            @Override
            public <T extends Searchable> int index(Iterable<T> iterable) {return 0;}

            @Override
            public <T extends Searchable> QueryResult<T> search(Class<T> aClass, Search search) {
              return null;
            }

            @Override
            public <T extends Searchable> void removeIndex(Class<T> aClass, Iterable<?> iterable) {}
          }, null));
    var changeTracker = new ChangeTracker( null, null, null, null);
    DAOFileDBImpl daoFileDB = new DAOFileDBImpl(changeTracker);
    BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);
    // refreshBooks(apiConnector, daoFileDB);
    ScriptureStore scriptureStore = new ScriptureStore(
        () -> dao, null, bibleBookStore, new ScriptureReferenceProcessorSplitImpl(bibleBookStore),
        new ScriptureFetcherRouter(apiConnector), null, null);

    scriptureStore.getScriptures(version, language, reference).getItems().forEach(System.out::println);
  }

  private static void refreshBooks(APIConnector apiConnector, DAOFileDBImpl daoFileDB) {
    String baseUrl = apiConnector.formatUrl("books");
    JSONArray array = new JSONObject(apiConnector.sendGet(String.format(BOOK_URL, baseUrl))).getJSONArray("items");
    daoFileDB.persist(
        Transaction.of(
            range(0, array.length())
                .mapToObj(i -> JSONUtil.toObject(BibleBook.class, array.getJSONObject(i)))
                .peek(System.out::println)
                .map(Op::create)
                .collect(toImmutableList())));
    daoFileDB.saveFiles();
  }
}
