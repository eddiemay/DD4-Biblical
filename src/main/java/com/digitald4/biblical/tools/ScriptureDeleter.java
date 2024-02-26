package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOApiImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import java.io.IOException;

public class ScriptureDeleter {
  private final ScriptureStore scriptureStore;

  private ScriptureDeleter(ScriptureStore scriptureStore) {
    this.scriptureStore = scriptureStore;
  }

  public void preview(String searchText) throws IOException {
    QueryResult<Scripture> result =
        scriptureStore.search(Query.forSearch(searchText).setPageSize(1000));
    result.getItems().forEach(System.out::println);
    System.out.printf("\nFound %d items to delete\n", result.getItems().size());
    if (result.getTotalSize() > 0) {
      System.out.println("Proceed with deletion? y/N");
      int read = System.in.read();
      if (read == 'Y' || read == 'y') {
        scriptureStore.delete(
            result.getItems().stream().map(Scripture::getId).collect(toImmutableList()));
      }
    }
  }

  public static void main(String[] args) throws IOException {
    String idToken = null;
    if (args.length == 0 || args[0].isEmpty()) {
      System.out.println("Usage ScriptureDeleter searchText [idtoken]");
      return;
    }

    String searchText = args[0];

    if (args.length == 2) {
      idToken = args[1];
    }

    APIConnector apiConnector =
        new APIConnector(Constants.API_URL, Constants.API_VERSION, 100).loadIdToken();
    DAOApiImpl dao = new DAOApiImpl(apiConnector);
    BibleBookStore bibleBookStore = new BibleBookStore(() -> dao);
    new ScriptureDeleter(
        new ScriptureStore(() -> dao, null, bibleBookStore,
            new ScriptureReferenceProcessorSplitImpl(bibleBookStore), null, null, null))
        .preview(searchText);
  }
}
